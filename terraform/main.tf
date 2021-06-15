locals {
  region = "eu-central-1"

  tags = {
    Environment = var.environment
  }
}

provider "aws" {
  profile = "default"
  region  = local.region
}

####################
## Target Groups ###
####################

resource "aws_lb_target_group" "sama_service_green" {
  name                 = "sama-service-green-tg-${var.environment}"
  protocol             = "HTTP"
  port                 = 3000
  vpc_id               = var.vpc_id
  deregistration_delay = 30
  target_type          = "instance"

  health_check {
    enabled             = true
    interval            = 15
    path                = "/__mon/health"
    port                = "traffic-port"
    healthy_threshold   = 3
    unhealthy_threshold = 5
    timeout             = 5
    protocol            = "HTTP"
    matcher             = "200-299"
  }

  tags = local.tags
}

resource "aws_lb_target_group" "sama_service_blue" {
  name                 = "sama-service-blue-tg-${var.environment}"
  protocol             = "HTTP"
  port                 = 3000
  vpc_id               = var.vpc_id
  deregistration_delay = 30
  target_type          = "instance"

  health_check {
    enabled             = true
    interval            = 15
    path                = "/__mon/health"
    port                = "traffic-port"
    healthy_threshold   = 3
    unhealthy_threshold = 5
    timeout             = 5
    protocol            = "HTTP"
    matcher             = "200-299"
  }
}


resource "aws_lb_listener_rule" "sama-service" {
  listener_arn = var.lb_listener_arn

  action {
    type = "forward"

    forward {
      target_group {
        arn    = aws_lb_target_group.sama_service_green.arn
        weight = lookup(local.traffic_dist_map[var.traffic_distribution], "green", 100)
      }
      target_group {
        arn    = aws_lb_target_group.sama_service_blue.arn
        weight = lookup(local.traffic_dist_map[var.traffic_distribution], "blue", 0)
      }
      stickiness {
        enabled  = false
        duration = 1
      }
    }
  }

  condition {
    path_pattern {
      values = ["/api/*"]
    }
  }
}

############
### ASGs ###
############

resource "aws_autoscaling_group" "green" {
  name = "sama-service-asg-green-${var.environment}"

  desired_capacity    = var.enable_green_env ? var.green_instance_count : 0
  min_size            = 0
  max_size            = 4
  vpc_zone_identifier = var.public_subnets
  target_group_arns = [
  aws_lb_target_group.sama_service_green.arn]

  health_check_grace_period = 90
  health_check_type         = "EC2"

  launch_template {
    id      = aws_launch_template.sama_service.id
    version = "$Latest"
  }

  tag {
    key                 = "Environment"
    value               = var.environment
    propagate_at_launch = true
  }
}

resource "aws_autoscaling_group" "blue" {
  name = "sama-service-asg-blue-${var.environment}"

  desired_capacity    = var.enable_blue_env ? var.blue_instance_count : 0
  min_size            = 0
  max_size            = 4
  vpc_zone_identifier = var.public_subnets
  target_group_arns = [
  aws_lb_target_group.sama_service_blue.arn]

  health_check_grace_period = 15
  health_check_type         = "ELB"

  launch_template {
    id      = aws_launch_template.sama_service.id
    version = "$Latest"
  }

  tag {
    key                 = "Environment"
    value               = var.environment
    propagate_at_launch = true
  }
}

resource "aws_launch_template" "sama_service" {
  name                   = "sama-service-lt-${var.environment}"
  image_id               = var.ami_id
  instance_type          = "t2.micro"
  key_name               = var.key_name
  update_default_version = true

  instance_initiated_shutdown_behavior = "terminate"

  iam_instance_profile {
    name = aws_iam_instance_profile.sama_service_asg.name
  }

  network_interfaces {
    associate_public_ip_address = true
    security_groups = [
    module.asg_sg.security_group_id]
  }

  block_device_mappings {
    device_name = "/dev/sda1"

    ebs {
      volume_size = 8
      volume_type = "gp2"
    }
  }

  tag_specifications {
    resource_type = "instance"

    tags = {
      Environment = var.environment
      Name        = "sama-service-${var.environment}"
    }
  }

  user_data = filebase64("${path.module}/scripts/deploy.sh")
}

module "asg_sg" {
  source = "terraform-aws-modules/security-group/aws"

  name        = "sama-service-asg-sg-${var.environment}"
  description = "Security group for ASGs"
  vpc_id      = var.vpc_id

  ingress_with_cidr_blocks = [
    {
      from_port   = 3000
      to_port     = 3000
      protocol    = "TCP"
      description = "application port"
      cidr_blocks = "10.0.0.0/16"
    },
    {
      from_port   = 9100
      to_port     = 9100
      protocol    = "TCP"
      description = "node exporter port"
      cidr_blocks = "10.0.0.0/16"
    },
    {
      from_port   = 22
      to_port     = 22
      protocol    = "TCP"
      description = "SSH"
      cidr_blocks = "0.0.0.0/0"
    }
  ]

  egress_with_cidr_blocks = [
    {
      from_port   = 0
      to_port     = 0
      protocol    = "-1"
      cidr_blocks = "0.0.0.0/0"
    }
  ]

  tags = local.tags
}


resource "aws_iam_instance_profile" "sama_service_asg" {
  name = "sama-service-asg-instance-profile-${var.environment}"
  role = aws_iam_role.sama_service_asg.name
}

resource "aws_iam_role" "sama_service_asg" {
  name = "sama-service-asg-role-${var.environment}"
  path = "/"

  managed_policy_arns = [
    "arn:aws:iam::aws:policy/service-role/AWSAppRunnerServicePolicyForECRAccess",
    aws_iam_policy.cloudwatch_logs.arn
  ]

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Sid    = ""
        Principal = {
          Service = "ec2.amazonaws.com"
        }
      }
    ]
  })

  tags = local.tags
}

resource "aws_iam_policy" "cloudwatch_logs" {
  name = "CloudwatchLogWriterForEC2"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect : "Allow",
        Action : [
          "logs:CreateLogGroup",
          "logs:CreateLogStream",
          "logs:PutLogEvents",
          "logs:DescribeLogStreams"
        ],
        Resource : [
          "arn:aws:logs:*:*:*"
        ]
      },
      {
        Effect : "Allow",
        Action : [
          "s3:GetObject"
        ],
        Resource : [
          "arn:aws:s3:::${var.cloudwatch-logs-bucket-name}/*"
        ]
      },
      {
        Effect : "Allow",
        Action : [
          "secretsmanager:GetResourcePolicy",
          "secretsmanager:GetSecretValue",
          "secretsmanager:DescribeSecret",
          "secretsmanager:ListSecretVersionIds",
          "secretsmanager:ListSecrets"
        ],
        Resource : [
          var.secret_manager_secret_arn
        ]
      }
    ]
  })
}
