terraform {
  backend "gcs" {}

  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "5.20.0"
    }

    random = {
      source  = "hashicorp/random"
      version = "3.6.0"
    }
  }
}

provider "google" {
  project = var.gcp_project_id
  region  = local.region
  zone    = var.zone
}

resource "random_id" "namespace" {
  byte_length = 8
}

locals {
  full_name = "${var.name}-${random_id.namespace.id}"
  region    = regex("(.*)-.*$", var.zone)[0]
}

module "vpc" {
  source        = "github.com/tilgalas/terraform-lib//modules/vpc"
  region        = local.region
  name          = local.full_name
  ip_cidr_range = "192.168.20.0/24"
}

data "google_compute_default_service_account" "default" {}

resource "google_compute_instance" "mq_instance" {
  name = var.name
  boot_disk {
    initialize_params {
      image = var.boot_disk_image
    }
  }
  machine_type = var.instance_type
  network_interface {
    subnetwork = module.vpc.subnet_id
  }

  service_account {
    email  = data.google_compute_default_service_account.default.email
    scopes = ["cloud-platform"]
  }

  metadata = {
    "startup-script" = file("../scripts/mq-install.sh")
  }

}
