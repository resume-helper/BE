#!/bin/bash
set -e

CURRENT_PORT=$(grep -oP '(?<=server 127.0.0.1:)\d+' /etc/nginx/conf.d/atomiccv.conf)
if [ "$CURRENT_PORT" == "8080" ]; then
  ROLLBACK_PORT=8081
  ROLLBACK_COLOR="green"
  CURRENT_COLOR="blue"
else
  ROLLBACK_PORT=8080
  ROLLBACK_COLOR="blue"
  CURRENT_COLOR="green"
fi

echo "▶ 롤백: $ROLLBACK_PORT ($ROLLBACK_COLOR) 재시작"
docker start "atomiccv-$ROLLBACK_COLOR"

# Health Check
for i in {1..10}; do
  STATUS=$(curl -s -o /dev/null -w "%{http_code}" "http://127.0.0.1:$ROLLBACK_PORT/actuator/health" 2>/dev/null || echo "000")
  if [ "$STATUS" == "200" ]; then
    echo "✅ Health Check 통과"
    break
  fi
  if [ "$i" -eq 10 ]; then
    echo "❌ 롤백 대상 컨테이너도 Health Check 실패"
    exit 1
  fi
  sleep 5
done

sudo sed -i "s/server 127.0.0.1:$CURRENT_PORT/server 127.0.0.1:$ROLLBACK_PORT/" /etc/nginx/conf.d/atomiccv.conf
sudo nginx -s reload

docker stop "atomiccv-$CURRENT_COLOR" 2>/dev/null || true

echo "✅ 롤백 완료 — Active Port: $ROLLBACK_PORT ($ROLLBACK_COLOR)"
