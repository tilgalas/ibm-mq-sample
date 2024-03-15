variable "gcp_project_id" {
  type        = string
  description = "GCP Project Id to work in"
}

variable "zone" {
  type        = string
  description = "GCP zone"
}

variable "name" {
  type        = string
  description = "GCE Instance name"
}

variable "boot_disk_image" {
  type        = string
  description = "GCE boot disk image"
  default     = "debian-12"
}

variable "instance_type" {
  type        = string
  description = "GCE instance type"
  default     = "n2-standard-2"
}
