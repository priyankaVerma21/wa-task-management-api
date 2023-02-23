provider "azurerm" {
  features {}

}

data "azurerm_key_vault" "wa_key_vault" {
  name                = "${var.product}-${var.env}"
  resource_group_name = "${var.product}-${var.env}"
}

data "azurerm_key_vault" "s2s_key_vault" {
  name                = "s2s-${var.env}"
  resource_group_name = "rpe-service-auth-provider-${var.env}"
}

data "azurerm_key_vault_secret" "s2s_secret" {
  key_vault_id = data.azurerm_key_vault.s2s_key_vault.id
  name         = "microservicekey-wa-task-management-api"
}

resource "azurerm_key_vault_secret" "s2s_secret_task_management_api" {
  name         = "s2s-secret-task-management-api"
  value        = data.azurerm_key_vault_secret.s2s_secret.value
  key_vault_id = data.azurerm_key_vault.wa_key_vault.id
}

locals {
  computed_tags = {
    lastUpdated = timestamp()
  }
  common_tags = merge(var.common_tags, local.computed_tags)
}

//Create Database
module "wa_task_management_api_database" {
  source             = "git@github.com:hmcts/cnp-module-postgres?ref=master"
  product            = var.product
  name               = "${var.postgres_db_component_name}-postgres-db"
  location           = var.location
  env                = var.env
  database_name      = var.postgresql_database_name
  postgresql_user    = var.postgresql_user
  postgresql_version = "11"
  common_tags        = local.common_tags
  subscription       = var.subscription
  sku_capacity       = var.database_sku_capacity
  sku_name           = var.database_sku_name
}

//New Azure Flexible database
module "wa_task_management_api_database_flexible" {
  source             = "git@github.com:hmcts/terraform-module-postgresql-flexible?ref=master"
  product            = var.product
  component          = var.component
  name               = "${var.postgres_db_component_name}-postgres-db-flexible"
  location           = var.location
  business_area      = var.business_area
  env                = var.env
  pgsql_databases = [
    {
      name : var.postgresql_database_name
    }
  ]
  pgsql_server_configuration = [
    {
      name  = "wal_level"
      value = "logical"
    }
  ]


  pgsql_version      = 14
  common_tags        = local.common_tags
  pgsql_sku          = var.pgsql_sku

}

//New Azure Flexible database replica
module "wa_task_management_api_database_flexible_replica" {
  source             = "git@github.com:hmcts/terraform-module-postgresql-flexible?ref=master"
  product            = var.product
  component          = var.component
  name               = "${var.postgres_db_component_name}-postgres-db-flexible-replica"
  location           = var.location
  business_area      = var.business_area
  env                = var.env
  pgsql_databases = [
    {
      name : var.postgresql_database_name
    }
  ]
  pgsql_version      = 14
  common_tags        = local.common_tags
  pgsql_sku          = var.pgsql_sku
  
}

//Save secrets in vault
resource "azurerm_key_vault_secret" "POSTGRES-USER" {
  name         = "${var.postgres_db_component_name}-POSTGRES-USER"
  value        = module.wa_task_management_api_database.user_name
  key_vault_id = data.azurerm_key_vault.wa_key_vault.id
}

resource "azurerm_key_vault_secret" "POSTGRES-PASS" {
  name         = "${var.postgres_db_component_name}-POSTGRES-PASS"
  value        = module.wa_task_management_api_database.postgresql_password
  key_vault_id = data.azurerm_key_vault.wa_key_vault.id
}

resource "azurerm_key_vault_secret" "POSTGRES_HOST" {
  name         = "${var.postgres_db_component_name}-POSTGRES-HOST"
  value        = module.wa_task_management_api_database.host_name
  key_vault_id = data.azurerm_key_vault.wa_key_vault.id
}

resource "azurerm_key_vault_secret" "POSTGRES_PORT" {
  name         = "${var.postgres_db_component_name}-POSTGRES-PORT"
  value        = module.wa_task_management_api_database.postgresql_listen_port
  key_vault_id = data.azurerm_key_vault.wa_key_vault.id
}

resource "azurerm_key_vault_secret" "POSTGRES_DATABASE" {
  name         = "${var.postgres_db_component_name}-POSTGRES-DATABASE"
  value        = module.wa_task_management_api_database.postgresql_database
  key_vault_id = data.azurerm_key_vault.wa_key_vault.id
}
