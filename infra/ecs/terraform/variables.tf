locals {
  region = "eu-central-1"

  env = yamldecode(file("./env/${terraform.workspace}/env.yaml"))

  tags = {
    Environment = terraform.workspace
  }
}