# Nginx TLS Certificates

Place local or production TLS files here:

- `fullchain.pem`
- `privkey.pem`

For local development, run:

```bash
bash bin/linux/nginx-dev-cert.sh localhost
```

The generated certificate is self-signed. For a desktop WebView to accept `https://localhost`, install/trust the certificate in the local OS trust store, or replace it with a certificate issued by a trusted CA.
