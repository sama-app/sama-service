terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 3.27"
    }
  }

  backend "s3" {
    bucket = "terraform-sama"
    key    = "dev/sama-service-ecs.tfstate"
    region = "eu-central-1"
  }

  required_version = ">= 1.0.0"
}