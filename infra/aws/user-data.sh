#!/bin/bash
# EC2 첫 부팅 시 실행되는 user-data 스크립트.
# Amazon Linux 2023 ARM (t4g.small) 기준. Docker + Compose + git 설치 후 사용자 작업 안내까지.
# 사용: EC2 launch wizard 의 "User data" 필드에 이 파일 내용을 그대로 붙여넣음.

set -euxo pipefail

LOG=/var/log/livelab-bootstrap.log
exec > >(tee -a "$LOG") 2>&1

echo "[livelab] bootstrap started at $(date)"

# 1) 시스템 업데이트 + 필수 패키지
dnf update -y
dnf install -y docker git

# 2) Docker 데몬 자동 시작
systemctl enable --now docker

# 3) ec2-user 가 sudo 없이 docker 명령 쓸 수 있게
usermod -aG docker ec2-user

# 4) Docker Compose v2 (CLI 플러그인 형태)
mkdir -p /usr/local/lib/docker/cli-plugins
curl -fsSL "https://github.com/docker/compose/releases/latest/download/docker-compose-linux-aarch64" \
  -o /usr/local/lib/docker/cli-plugins/docker-compose
chmod +x /usr/local/lib/docker/cli-plugins/docker-compose

# 5) Cloudflare Tunnel 클라이언트 (선택 — 포트 노출 우회 옵션)
# curl -fsSL https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-arm64.rpm \
#   -o /tmp/cloudflared.rpm && dnf install -y /tmp/cloudflared.rpm

# 6) 안내 메시지 (SSH 로 들어왔을 때 보이게)
cat > /etc/motd <<'EOF'

  민야령 Backend Live Lab — EC2 부트스트랩 완료.

  다음 단계 (ec2-user 로):
    1) cd ~ && git clone https://github.com/minya8703/live-lab.git && cd live-lab
    2) cp .env.example .env && vi .env   # GOOGLE_API_KEY + POSTGRES_PASSWORD 설정
    3) docker compose --profile prod up -d
    4) docker compose logs -f app

  로그: /var/log/livelab-bootstrap.log

EOF

echo "[livelab] bootstrap complete at $(date)"
