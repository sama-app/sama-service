provider "aws" {
  profile = "default"
  region  = local.region
}

###########
### ALB ###
###########

data "aws_vpc" "selected" {
  id = local.env.vpc_id
}

resource "aws_lb_target_group" "sama_service" {
  name                 = "sama-service-ecs-tg-${terraform.workspace}"
  protocol             = "HTTP"
  port                 = 8080
  vpc_id               = data.aws_vpc.selected.id
  deregistration_delay = 10
  target_type          = "ip"

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


resource "aws_lb_listener_rule" "sama-service" {
  listener_arn = local.env.lb_listener_arn

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.sama_service.arn
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
  condition {
    http_header {
      http_header_name = "X-Alb-Secure"
      values           = [local.env.cf_to_alb_secure_header_value]
    }
  }
}

###########
### ECS ###
###########

data "aws_ecs_cluster" "selected" {
  cluster_name = "ecs-${terraform.workspace}"
}

data "aws_iam_role" "execution" {
  name = "ecs-task-execution-${terraform.workspace}"
}


resource "aws_ecs_task_definition" "sama_service" {
  family = "sama-service-${terraform.workspace}"
  cpu    = local.env.cpu
  memory = local.env.memory

  execution_role_arn = data.aws_iam_role.execution.arn
  task_role_arn      = aws_iam_role.sama_service.arn
  network_mode       = "awsvpc"

  container_definitions = jsonencode([
    {
      name      = "sama-service"
      image     = "216862985054.dkr.ecr.eu-central-1.amazonaws.com/sama-service:latest"
      essential = true
      environment = [
        {
          name  = "X_JAVA_OPTS"
          value = "-Dspring.profiles.active=${terraform.workspace}"
        }
      ]
      portMappings = [
        {
          containerPort = 3000
          hostPort      = 3000
        }
      ]
      healthCheck = {
        command     = ["CMD-SHELL", "wget -qO- http://localhost:3000/__mon/health || exit 1"]
        interval    = 15
        retries     = 5
        startPeriod = 90
      }
      logConfiguration = {
        logDriver = "awsfirelens"
        options = {
          Name     = "newrelic"
          apiKey   = "NEW_RELIC_API_KEY_DONT_WORRY_THE_OLD_ONE_IS_DISABLED"
          endpoint = "https://log-api.eu.newrelic.com/log/v1"
        }
      }
      dockerLabels = {
        PROMETHEUS_EXPORTER_PATH = "/__mon/prometheus"
        PROMETHEUS_EXPORTER_PORT = "3000"
      }
    },
    {
      name      = "sama-webserver"
      image     = "216862985054.dkr.ecr.eu-central-1.amazonaws.com/sama-webserver:latest"
      essential = true
      portMappings = [
        {
          containerPort = 8080
          hostPort      = 8080
        }
      ],
      dependsOn = [
        {
          containerName : "sama-service",
          condition : "START"
        }
      ]
      healthCheck = {
        command     = ["CMD-SHELL", "curl -f http://localhost:8080/ping || exit 1"]
        interval    = 15
        retries     = 5
        startPeriod = 15
      }
      logConfiguration = {
        logDriver = "awsfirelens"
        options = {
          Name     = "newrelic"
          apiKey   = "eu01xx251df55d793dd5a5d3416900aded02NRAL"
          endpoint = "https://log-api.eu.newrelic.com/log/v1"
        }
      }
    },
    {
      name      = "newrelic-logger"
      essential = true
      image     = "533243300146.dkr.ecr.eu-central-1.amazonaws.com/newrelic/logging-firelens-fluentbit"
      firelensConfiguration = {
        "type" = "fluentbit"
        "options" : {
          "enable-ecs-log-metadata" : "true"
        }
      }
      logConfiguration = {
        logDriver = "awslogs",
        options = {
          awslogs-group : terraform.workspace,
          awslogs-region : "eu-central-1",
          awslogs-stream-prefix : "ecs"
        }
      }
    }
  ])

  requires_compatibilities = ["FARGATE"]

  tags = local.tags
}


resource "aws_ecs_service" "sama_service" {
  name                              = "sama-service"
  cluster                           = data.aws_ecs_cluster.selected.id
  task_definition                   = aws_ecs_task_definition.sama_service.arn
  desired_count                     = 0
  depends_on                        = [aws_iam_policy.sama_service]
  health_check_grace_period_seconds = 150

  capacity_provider_strategy {
    capacity_provider = "FARGATE"
    weight            = local.env.fargate_weight
    base              = local.env.fargate_instance_base
  }

  capacity_provider_strategy {
    capacity_provider = "FARGATE_SPOT"
    weight            = local.env.fargate_spot_weight
    base              = local.env.fargate_spot_instance_base
  }

  deployment_circuit_breaker {
    enable   = true
    rollback = true
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.sama_service.arn
    container_name   = "sama-webserver"
    container_port   = 8080
  }

  network_configuration {
    subnets          = local.env.subnets
    security_groups  = [module.security_group.security_group_id]
    assign_public_ip = true # required when using a public subnet
  }

  lifecycle {
    ignore_changes = [desired_count, task_definition]
  }

  tags = local.tags
}

module "security_group" {
  source = "terraform-aws-modules/security-group/aws"

  name        = "sama-service-ecs-sg-${terraform.workspace}"
  description = "Security group for sama-servce ECS"
  vpc_id      = data.aws_vpc.selected.id

  ingress_with_cidr_blocks = [
    {
      from_port   = 3000
      to_port     = 3000
      protocol    = "TCP"
      description = "application port"
      cidr_blocks = data.aws_vpc.selected.cidr_block
    },
    {
      from_port   = 8080
      to_port     = 8080
      protocol    = "TCP"
      description = "webserver port"
      cidr_blocks = data.aws_vpc.selected.cidr_block
    },
    {
      from_port   = 22
      to_port     = 22
      protocol    = "TCP"
      description = "SSH"
      cidr_blocks = "0.0.0.0/0"
      # TODO: Restrict for extra security
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

resource "aws_appautoscaling_target" "sama_service" {
  max_capacity       = 5
  min_capacity       = 1
  resource_id        = "service/${data.aws_ecs_cluster.selected.cluster_name}/${aws_ecs_service.sama_service.name}"
  scalable_dimension = "ecs:service:DesiredCount"
  service_namespace  = "ecs"
}

resource "aws_appautoscaling_policy" "sama_service" {
  name               = "sama-service-ecs-${terraform.workspace}"
  policy_type        = "TargetTrackingScaling"
  resource_id        = aws_appautoscaling_target.sama_service.resource_id
  scalable_dimension = aws_appautoscaling_target.sama_service.scalable_dimension
  service_namespace  = aws_appautoscaling_target.sama_service.service_namespace

  target_tracking_scaling_policy_configuration {
    predefined_metric_specification {
      predefined_metric_type = "ECSServiceAverageCPUUtilization"
    }
    target_value       = 40
    scale_in_cooldown  = 60
    scale_out_cooldown = 60
  }
}

###########
### IAM ###
###########
resource "aws_iam_role" "sama_service" {
  name = "sama-service-ecs-${terraform.workspace}"
  path = "/"

  managed_policy_arns = [aws_iam_policy.sama_service.arn]

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Sid    = ""
        Principal = {
          Service = "ecs-tasks.amazonaws.com"
        }
      }
    ]
  })

  tags = local.tags
}

resource "aws_iam_policy" "sama_service" {
  name = "sama-service-ecs-${terraform.workspace}"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
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
      },
      {
        Effect : "Allow",
        Action : ["firehose:PutRecordBatch"],
        Resource : ["*"]
      }
    ]
  })
}
