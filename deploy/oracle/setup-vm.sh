#!/usr/bin/env bash
# Preparação única da VM Oracle Cloud Always Free (Ubuntu 22.04/24.04, Ampere A1 arm64).
# Rode como o usuário padrão (ubuntu): bash setup-vm.sh
set -euo pipefail

# Docker Engine + plugin compose (repositório oficial).
if ! command -v docker >/dev/null; then
  curl -fsSL https://get.docker.com | sudo sh
  sudo usermod -aG docker "$USER"
fi

# As imagens Ubuntu da Oracle trazem regras iptables que rejeitam tudo além do SSH, mesmo com a
# Security List liberada no console. Abre 80 e 443 (TCP e UDP para HTTP/3) antes da regra REJECT.
for rule in "tcp 80" "tcp 443" "udp 443"; do
  set -- $rule
  if ! sudo iptables -C INPUT -p "$1" --dport "$2" -m conntrack --ctstate NEW -j ACCEPT 2>/dev/null; then
    sudo iptables -I INPUT 5 -p "$1" --dport "$2" -m conntrack --ctstate NEW -j ACCEPT
  fi
done
sudo DEBIAN_FRONTEND=noninteractive apt-get install -y iptables-persistent >/dev/null
sudo netfilter-persistent save

# Swap de 4 GB: picos do OCR e da compilação do PDF não derrubam a JVM em instâncias menores.
if ! swapon --show | grep -q /swapfile; then
  sudo fallocate -l 4G /swapfile
  sudo chmod 600 /swapfile
  sudo mkswap /swapfile
  sudo swapon /swapfile
  echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab >/dev/null
fi

mkdir -p ~/tailorly
echo "Pronto. Crie ~/tailorly/.env a partir de deploy/oracle/.env.example (chmod 600)."
echo "Saia e entre de novo na sessão SSH para o grupo docker valer."
