locals {
  region = "eu-central-1"

  env = yamldecode(file("./env/${terraform.workspace}/env.yaml"))

  tags = {
    Environment = terraform.workspace
  }
}

##################
### Deployment ###
##################

variable "ami_id" {
  type    = string
  default = "ami-00b6106073860d6ac"
}

variable "enable_green_env" {
  description = "Enable green environment"
  type        = bool
  default     = true
}

variable "enable_blue_env" {
  description = "Enable blue environment"
  type        = bool
  default     = false
}

locals {
  traffic_dist_map = {
    blue = {
      blue  = 100
      green = 0
    }
    green = {
      blue  = 0
      green = 100
    }
    split = {
      blue  = 50
      green = 50
    }
  }
}

variable "traffic_distribution" {
  description = "Blue / Green ASGs traffic distribution"
  type        = string
}