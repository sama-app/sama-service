provider "aws" {
  profile = "default"
  region  = local.region
}

####################
## Target Groups ###
####################

resource "aws_lb_target_group" "sama_service_green" {
  name                 = "sama-service-green-tg-${terraform.workspace}"
  protocol             = "HTTP"
  port                 = 3000
  vpc_id               = local.env.vpc_id
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
  name                 = "sama-service-blue-tg-${terraform.workspace}"
  protocol             = "HTTP"
  port                 = 3000
  vpc_id               = local.env.vpc_id
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
  listener_arn = local.env.lb_listener_arn

  # Ensure ASGs are in place before target groups switch traffic
  depends_on = [aws_autoscaling_group.blue, aws_autoscaling_group.green]

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
  condition {
    host_header {
      values = [local.env.service_domain]
    }
  }
}

############
### ASGs ###
############

resource "aws_autoscaling_group" "green" {
  name = "sama-service-asg-green-${terraform.workspace}"

  desired_capacity      = var.enable_green_env ? local.env.instance_count : 0
  wait_for_elb_capacity = var.enable_green_env ? local.env.instance_count : 0
  min_size              = 0
  max_size              = 4

  vpc_zone_identifier = local.env.subnets
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
    value               = terraform.workspace
    propagate_at_launch = true
  }
}

resource "aws_autoscaling_group" "blue" {
  name = "sama-service-asg-blue-${terraform.workspace}"

  desired_capacity      = var.enable_blue_env ? local.env.instance_count : 0
  wait_for_elb_capacity = var.enable_blue_env ? local.env.instance_count : 0
  min_size              = 0
  max_size              = 4

  vpc_zone_identifier = local.env.subnets
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
    value               = terraform.workspace
    propagate_at_launch = true
  }
}

resource "aws_launch_template" "sama_service" {
  name                   = "sama-service-lt-${terraform.workspace}"
  image_id               = var.ami_id
  instance_type          = local.env.instance_type
  key_name               = local.env.key_name
  update_default_version = true

  instance_initiated_shutdown_behavior = "terminate"

  iam_instance_profile {
    name = aws_iam_instance_profile.sama_service_asg.name
  }

  network_interfaces {
    associate_public_ip_address = local.env.publicly_accessible
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
      Environment = terraform.workspace
      Name        = "sama-service-${terraform.workspace}"
    }
  }

  user_data = filebase64("${path.module}/scripts/deploy_${terraform.workspace}.sh")
}

module "asg_sg" {
  source = "terraform-aws-modules/security-group/aws"

  name        = "sama-service-asg-sg-${terraform.workspace}"
  description = "Security group for ASGs"
  vpc_id      = local.env.vpc_id

  ingress_with_cidr_blocks = [
    {
      from_port   = 3000
      to_port     = 3000
      protocol    = "TCP"
      description = "application port"
      cidr_blocks = local.env.vpc_cidr_block
    },
    {
      from_port   = 9100
      to_port     = 9100
      protocol    = "TCP"
      description = "node exporter port"
      cidr_blocks = local.env.vpc_cidr_block
    },
    {
      from_port   = 22
      to_port     = 22
      protocol    = "TCP"
      description = "SSH"
      cidr_blocks = "0.0.0.0/0" # TODO: Restrict for extra security
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
  name = "sama-service-asg-instance-profile-${terraform.workspace}"
  role = aws_iam_role.sama_service_asg.name
}

resource "aws_iam_role" "sama_service_asg" {
  name = "sama-service-asg-role-${terraform.workspace}"
  path = "/"

  managed_policy_arns = [
    "arn:aws:iam::aws:policy/service-role/AWSAppRunnerServicePolicyForECRAccess",
    aws_iam_policy.sama_service_asg.arn
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

resource "aws_iam_policy" "sama_service_asg" {
  name = "sama-service-iam-policy-${terraform.workspace}"

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
          "secretsmanager:GetResourcePolicy",
          "secretsmanager:GetSecretValue",
          "secretsmanager:DescribeSecret",
          "secretsmanager:ListSecretVersionIds",
          "secretsmanager:ListSecrets"
        ],
        Resource : [
          local.env.secret_manager_secret_arn
        ]
      }
    ]
  })
}
