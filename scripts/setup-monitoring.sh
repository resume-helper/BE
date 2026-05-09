#!/bin/bash
# Grafana Alloy 설치 및 모니터링 에이전트 설정
# Usage: ./setup-monitoring.sh <loki_url> <loki_user> <loki_password> <metrics_url> <metrics_user> <metrics_password>
set -e

if [ $# -ne 6 ]; then
  echo "Usage: $0 <loki_url> <loki_user> <loki_password> <metrics_url> <metrics_user> <metrics_password>"
  exit 1
fi

LOKI_URL=$1
LOKI_USER=$2
LOKI_PASSWORD=$3
METRICS_URL=$4
METRICS_USER=$5
METRICS_PASSWORD=$6

# Grafana Alloy 설치
echo "▶ Grafana Alloy 설치"
sudo apt-get install -y gpg wget
wget -q -O - https://apt.grafana.com/gpg.key | gpg --dearmor | sudo tee /etc/apt/keyrings/grafana.gpg > /dev/null
echo "deb [signed-by=/etc/apt/keyrings/grafana.gpg] https://apt.grafana.com stable main" | sudo tee /etc/apt/sources.list.d/grafana.list
sudo apt-get update -q
sudo apt-get install -y alloy

# Docker 소켓 접근 권한 (로그 수집용)
sudo usermod -aG docker alloy

# 인증 정보 파일 생성
echo "▶ 인증 정보 저장"
sudo mkdir -p /etc/alloy
sudo tee /etc/alloy/credentials.env > /dev/null <<EOF
LOKI_URL=${LOKI_URL}
LOKI_USER=${LOKI_USER}
LOKI_PASSWORD=${LOKI_PASSWORD}
METRICS_URL=${METRICS_URL}
METRICS_USER=${METRICS_USER}
METRICS_PASSWORD=${METRICS_PASSWORD}
EOF
sudo chmod 600 /etc/alloy/credentials.env

# Alloy 설정 파일 생성
echo "▶ Alloy 설정 파일 작성"
sudo tee /etc/alloy/config.alloy > /dev/null <<'ALLOYCONFIG'
// ─── Docker 로그 수집 → Loki ───────────────────────────────────────────────

discovery.docker "atomiccv" {
  host = "unix:///var/run/docker.sock"
  filter {
    name   = "name"
    values = ["atomiccv-blue", "atomiccv-green"]
  }
}

loki.source.docker "atomiccv_logs" {
  host       = "unix:///var/run/docker.sock"
  targets    = discovery.docker.atomiccv.targets
  forward_to = [loki.process.parse_level.receiver]
}

// ECS JSON 로그에서 log.level 추출 → Loki 라벨
loki.process "parse_level" {
  forward_to = [loki.write.grafana_cloud.receiver]

  stage.json {
    expressions = { level = "log.level" }
  }
  stage.labels {
    values = { level = "" }
  }
  stage.static_labels {
    values = { app = "atomiccv", env = "prod" }
  }
}

loki.write "grafana_cloud" {
  endpoint {
    url = env("LOKI_URL")
    basic_auth {
      username = env("LOKI_USER")
      password = env("LOKI_PASSWORD")
    }
  }
}

// ─── Spring Boot 메트릭 (Prometheus scrape) ────────────────────────────────

prometheus.scrape "spring_boot" {
  targets = [
    {"__address__" = "localhost:8080", "color" = "blue"},
    {"__address__" = "localhost:8081", "color" = "green"},
  ]
  scrape_interval = "15s"
  metrics_path    = "/actuator/prometheus"
  honor_labels    = true
  forward_to      = [prometheus.remote_write.grafana_cloud.receiver]
}

// ─── EC2 시스템 메트릭 (Node Exporter 내장) ───────────────────────────────

prometheus.exporter.unix "node" {}

prometheus.scrape "node_exporter" {
  targets    = prometheus.exporter.unix.node.targets
  forward_to = [prometheus.remote_write.grafana_cloud.receiver]
}

// ─── Prometheus Remote Write → Grafana Cloud ──────────────────────────────

prometheus.remote_write "grafana_cloud" {
  endpoint {
    url = env("METRICS_URL")
    basic_auth {
      username = env("METRICS_USER")
      password = env("METRICS_PASSWORD")
    }
  }
  external_labels = { app = "atomiccv", env = "prod" }
}
ALLOYCONFIG

# systemd 환경변수 주입 설정
echo "▶ systemd 환경변수 설정"
sudo mkdir -p /etc/systemd/system/alloy.service.d
sudo tee /etc/systemd/system/alloy.service.d/override.conf > /dev/null <<EOF
[Service]
EnvironmentFile=/etc/alloy/credentials.env
ExecStart=
ExecStart=/usr/bin/alloy run /etc/alloy/config.alloy
EOF

# 서비스 시작
echo "▶ Alloy 서비스 시작"
sudo systemctl daemon-reload
sudo systemctl enable alloy
sudo systemctl restart alloy

echo "✅ Grafana Alloy 설치 완료"
echo "상태 확인: sudo systemctl status alloy"
