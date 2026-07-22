update channel_accounts
set base_url = 'https://gql-beta.hashnode.com/'
where type = 'HASHNODE' and base_url in ('https://gql.hashnode.com', 'https://gql.hashnode.com/');
