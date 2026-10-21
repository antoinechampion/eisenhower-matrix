-- Idempotent schema initialization for Spring Authorization Server on SQL Server.
-- Note: varchar(max) is used for LOB columns (attributes, tokens, and metadata) instead of varbinary(max)
-- because Spring Authorization Server's JdbcOAuth2AuthorizationService binds and reads JSON/token strings
-- directly via standard character JDBC methods (Types.VARCHAR / Types.LONGVARCHAR). Using varbinary(max)
-- causes the Microsoft SQL Server JDBC driver to treat bound strings as hex, breaking token persistence.

IF OBJECT_ID(N'oauth2_registered_client', N'U') IS NULL
CREATE TABLE oauth2_registered_client (
    id varchar(100) NOT NULL,
    client_id varchar(100) NOT NULL,
    client_id_issued_at datetime2 DEFAULT CURRENT_TIMESTAMP NOT NULL,
    client_secret varchar(200) DEFAULT NULL,
    client_secret_expires_at datetime2 DEFAULT NULL,
    client_name varchar(200) NOT NULL,
    client_authentication_methods varchar(1000) NOT NULL,
    authorization_grant_types varchar(1000) NOT NULL,
    redirect_uris varchar(1000) DEFAULT NULL,
    post_logout_redirect_uris varchar(1000) DEFAULT NULL,
    scopes varchar(1000) NOT NULL,
    client_settings varchar(2000) NOT NULL,
    token_settings varchar(2000) NOT NULL,
    PRIMARY KEY (id)
);

IF OBJECT_ID(N'oauth2_authorization', N'U') IS NULL
CREATE TABLE oauth2_authorization (
    id varchar(100) NOT NULL,
    registered_client_id varchar(100) NOT NULL,
    principal_name varchar(200) NOT NULL,
    authorization_grant_type varchar(100) NOT NULL,
    authorized_scopes varchar(1000) DEFAULT NULL,
    attributes varchar(max) DEFAULT NULL,
    state varchar(500) DEFAULT NULL,
    authorization_code_value varchar(max) DEFAULT NULL,
    authorization_code_issued_at datetime2 DEFAULT NULL,
    authorization_code_expires_at datetime2 DEFAULT NULL,
    authorization_code_metadata varchar(max) DEFAULT NULL,
    access_token_value varchar(max) DEFAULT NULL,
    access_token_issued_at datetime2 DEFAULT NULL,
    access_token_expires_at datetime2 DEFAULT NULL,
    access_token_metadata varchar(max) DEFAULT NULL,
    access_token_type varchar(100) DEFAULT NULL,
    access_token_scopes varchar(1000) DEFAULT NULL,
    oidc_id_token_value varchar(max) DEFAULT NULL,
    oidc_id_token_issued_at datetime2 DEFAULT NULL,
    oidc_id_token_expires_at datetime2 DEFAULT NULL,
    oidc_id_token_metadata varchar(max) DEFAULT NULL,
    refresh_token_value varchar(max) DEFAULT NULL,
    refresh_token_issued_at datetime2 DEFAULT NULL,
    refresh_token_expires_at datetime2 DEFAULT NULL,
    refresh_token_metadata varchar(max) DEFAULT NULL,
    user_code_value varchar(max) DEFAULT NULL,
    user_code_issued_at datetime2 DEFAULT NULL,
    user_code_expires_at datetime2 DEFAULT NULL,
    user_code_metadata varchar(max) DEFAULT NULL,
    device_code_value varchar(max) DEFAULT NULL,
    device_code_issued_at datetime2 DEFAULT NULL,
    device_code_expires_at datetime2 DEFAULT NULL,
    device_code_metadata varchar(max) DEFAULT NULL,
    PRIMARY KEY (id)
);
