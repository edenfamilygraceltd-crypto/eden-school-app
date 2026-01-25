# 🔐 CONFIGURATION DE SÉCURITÉ SERVEUR

## Instructions pour configurer la sécurité côté serveur

### Si vous utilisez Node.js/Express

Installez `helmet`:
```bash
npm install helmet
```

Ajoutez au début de votre `server.js`:
```javascript
const helmet = require('helmet');
const express = require('express');
const app = express();

// 🔒 Sécurité complète avec helmet
app.use(helmet());

// 🔒 Content Security Policy (CSP)
app.use(helmet.contentSecurityPolicy({
  directives: {
    defaultSrc: ["'self'"],
    scriptSrc: ["'self'", "https://cdn.jsdelivr.net", "https://www.gstatic.com"],
    styleSrc: ["'self'", "https://cdn.jsdelivr.net"],
    imgSrc: ["'self'", "data:", "https:"],
    connectSrc: ["'self'", "https://www.googleapis.com"],
    fontSrc: ["'self'", "https://fonts.googleapis.com"],
    frameSrc: ["'none'"],
    baseUri: ["'self'"],
    formAction: ["'self'"]
  }
}));

// 🔒 Empêcher les clics d'être intégrés dans d'autres sites
app.use(helmet.frameguard({ action: 'deny' }));

// 🔒 Empêcher les navigateurs de deviner le type MIME
app.use(helmet.noSniff());

// 🔒 Protection XSS
app.use(helmet.xssFilter());

// 🔒 HSTS (Force HTTPS)
app.use(helmet.hsts({
  maxAge: 31536000,        // 1 an
  includeSubDomains: true,
  preload: true
}));

app.listen(3000);
```

---

### Si vous utilisez Node.js/Koa

```javascript
const Koa = require('koa');
const helmet = require('koa-helmet');
const app = new Koa();

app.use(helmet());
app.use(helmet.contentSecurityPolicy({
  directives: {
    defaultSrc: ["'self'"],
    scriptSrc: ["'self'", "https://cdn.jsdelivr.net"],
    styleSrc: ["'self'", "https://cdn.jsdelivr.net"],
  }
}));
```

---

### Si vous utilisez PHP

Ajoutez au début de votre fichier `.htaccess` ou dans le header PHP:

```apache
# .htaccess
Header always set X-Content-Type-Options "nosniff"
Header always set X-Frame-Options "DENY"
Header always set X-XSS-Protection "1; mode=block"
Header always set Strict-Transport-Security "max-age=31536000; includeSubDomains; preload"
Header always set Content-Security-Policy "default-src 'self'; script-src 'self' https://cdn.jsdelivr.net https://www.gstatic.com; style-src 'self' https://cdn.jsdelivr.net;"
Header always set Referrer-Policy "strict-origin-when-cross-origin"
Header always set Permissions-Policy "geolocation=(), microphone=(), camera=()"
```

**Ou en PHP:**
```php
<?php
// En haut de chaque page
header("X-Content-Type-Options: nosniff");
header("X-Frame-Options: DENY");
header("X-XSS-Protection: 1; mode=block");
header("Strict-Transport-Security: max-age=31536000; includeSubDomains; preload");
header("Content-Security-Policy: default-src 'self'; script-src 'self' https://cdn.jsdelivr.net https://www.gstatic.com; style-src 'self' https://cdn.jsdelivr.net;");
header("Referrer-Policy: strict-origin-when-cross-origin");
header("Permissions-Policy: geolocation=(), microphone=(), camera=()");
?>
```

---

### Si vous utilisez Python/Flask

```python
from flask import Flask
from flask_talisman import Talisman

app = Flask(__name__)

# 🔒 Configuration de sécurité complète
Talisman(app, 
    force_https=True,
    strict_transport_security=True,
    strict_transport_security_max_age=31536000,
    content_security_policy={
        'default-src': "'self'",
        'script-src': ["'self'", "https://cdn.jsdelivr.net", "https://www.gstatic.com"],
        'style-src': ["'self'", "https://cdn.jsdelivr.net"],
        'img-src': ["'self'", "data:", "https:"],
    }
)

if __name__ == '__main__':
    app.run(ssl_context='adhoc')  # Force HTTPS
```

---

### Si vous utilisez Django

Ajoutez à `settings.py`:

```python
# 🔒 Configuration de sécurité
SECURE_BROWSER_XSS_FILTER = True
SECURE_CONTENT_SECURITY_POLICY = {
    "default-src": ("'self'",),
    "script-src": ("'self'", "https://cdn.jsdelivr.net", "https://www.gstatic.com"),
    "style-src": ("'self'", "https://cdn.jsdelivr.net"),
    "img-src": ("'self'", "data:", "https:"),
}

SECURE_HSTS_SECONDS = 31536000
SECURE_HSTS_INCLUDE_SUBDOMAINS = True
SECURE_HSTS_PRELOAD = True
SECURE_SSL_REDIRECT = True
SESSION_COOKIE_SECURE = True
CSRF_COOKIE_SECURE = True
X_FRAME_OPTIONS = "DENY"
```

---

### Si vous utilisez Nginx

Ajoutez à votre configuration `nginx.conf`:

```nginx
server {
    listen 443 ssl http2;
    server_name example.com;

    # 🔒 En-têtes de sécurité
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-Frame-Options "DENY" always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains; preload" always;
    add_header Content-Security-Policy "default-src 'self'; script-src 'self' https://cdn.jsdelivr.net https://www.gstatic.com; style-src 'self' https://cdn.jsdelivr.net;" always;
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;
    add_header Permissions-Policy "geolocation=(), microphone=(), camera=()" always;

    # 🔒 SSL/TLS Configuration
    ssl_certificate /path/to/cert.pem;
    ssl_certificate_key /path/to/key.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;

    # 🔒 Redirection HTTP vers HTTPS
    if ($scheme != "https") {
        return 301 https://$server_name$request_uri;
    }

    location / {
        proxy_pass http://localhost:3000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

---

### Si vous utilisez Apache

Ajoutez à votre configuration Apache ou `.htaccess`:

```apache
# Vérifier que mod_rewrite est activé
<IfModule mod_rewrite.c>
    RewriteEngine On
    # 🔒 Forcer HTTPS
    RewriteCond %{HTTPS} off
    RewriteRule ^(.*)$ https://%{HTTP_HOST}%{REQUEST_URI} [L,R=301]
</IfModule>

# 🔒 En-têtes de sécurité
<IfModule mod_headers.c>
    Header always set X-Content-Type-Options "nosniff"
    Header always set X-Frame-Options "DENY"
    Header always set X-XSS-Protection "1; mode=block"
    Header always set Strict-Transport-Security "max-age=31536000; includeSubDomains; preload"
    Header always set Content-Security-Policy "default-src 'self'; script-src 'self' https://cdn.jsdelivr.net https://www.gstatic.com; style-src 'self' https://cdn.jsdelivr.net;"
    Header always set Referrer-Policy "strict-origin-when-cross-origin"
</IfModule>

# 🔒 Désactiver les fichiers .htaccess
<FilesMatch "\.htaccess$">
    Order allow,deny
    Deny from all
</FilesMatch>

# 🔒 Protéger les fichiers sensibles
<FilesMatch "\.(env|config|sql|log)$">
    Order allow,deny
    Deny from all
</FilesMatch>
```

---

## 🧪 Tester la configuration de sécurité

### Test 1: Vérifier HTTPS
```bash
curl -I https://example.com
# Doit retourner 200 (pas 301)
```

### Test 2: Vérifier les en-têtes
```bash
curl -I https://example.com | grep -i "x-content-type\|x-frame\|strict-transport\|csp"
# Doit afficher tous les en-têtes
```

### Test 3: Scanner de sécurité
```bash
# Utiliser https://www.ssllabs.com/ssltest/
# Score attendu: A ou A+
```

### Test 4: Vérifier CSP
```bash
curl -I https://example.com | grep -i "content-security-policy"
# Doit afficher la CSP complète
```

---

## 📊 Checklist de configuration

- [ ] HTTPS forcé (port 443)
- [ ] Certificat SSL/TLS valide et à jour
- [ ] TLS 1.2 ou supérieur uniquement
- [ ] X-Content-Type-Options configuré
- [ ] X-Frame-Options configuré
- [ ] Content-Security-Policy configuré
- [ ] Strict-Transport-Security configuré (HSTS)
- [ ] Referrer-Policy configuré
- [ ] Permissions-Policy configuré
- [ ] Redirection HTTP → HTTPS en place
- [ ] Cookies sécurisés (HttpOnly, Secure, SameSite)
- [ ] CORS correctement configuré
- [ ] Rate limiting en place
- [ ] Logs de sécurité en place

---

## 🔗 Ressources

- **OWASP Secure Headers**: https://owasp.org/www-project-secure-headers/
- **Mozilla Observatory**: https://observatory.mozilla.org/
- **SSL Labs**: https://www.ssllabs.com/ssltest/
- **CSP Evaluator**: https://csp-evaluator.withgoogle.com/

---

**Mise à jour: 25 Janvier 2026**
