environment = "dev"

vpc_id = "vpc-016aad6a1a3fd88cf"
public_subnets = [
  "subnet-0d32617a1bd265b95",
"subnet-08ac263cc0c45fed6"]
secret_manager_secret_arn = "arn:aws:secretsmanager:eu-central-1:216862985054:secret:secret/sama-service_dev-F3IjDc"

lb_arn          = "arn:aws:elasticloadbalancing:eu-central-1:216862985054:loadbalancer/app/alb-dev/bb33b81f806004ec"
lb_listener_arn = "arn:aws:elasticloadbalancing:eu-central-1:216862985054:listener/app/alb-dev/bb33b81f806004ec/3c833319e875613d"