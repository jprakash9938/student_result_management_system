# Deploying Student Result Management System to Render

This guide provides step-by-step instructions for deploying your Spring Boot + PostgreSQL application to [Render](https://render.com).

---

## Why "Docker" is Selected in Render

Render does not offer a standalone "Java" option in its manual runtime dropdown menu (it only lists Node, Python, Go, Rust, Ruby, Elixir, and Docker). 

Because your project contains a multi-stage **`Dockerfile`**, selecting **Docker** allows Render to automatically build and run your Spring Boot application using Java 21!

---

## Method 1: Deploy with Render Blueprint (Easiest)

Render Blueprints automatically set up both the **PostgreSQL Database** and the **Web Service** using [`render.yaml`](file:///d:/PROJECTS/Result/render.yaml).

1. Push your repository to GitHub / GitLab.
2. Go to [Render Dashboard](https://dashboard.render.com/) -> **New +** -> **Blueprint**.
3. Connect your repository. Render will automatically detect `render.yaml` and provision both services.

---

## Method 2: Manual Web Service Setup on Render

If you are creating the Web Service manually from the screen shown in your screenshot:

### Step 1: Runtime Selection
1. In the **Language / Runtime** dropdown, select **Docker** (as shown in your screenshot).
2. Leave **Dockerfile Path** as `Dockerfile` (or blank, as `Dockerfile` in root is detected automatically).

### Step 2: Add Environment Variables
Scroll down to the **Environment Variables** section and add:

| Key | Value / Instructions |
|---|---|
| `PORT` | `8080` |
| `DB_HOST` | Internal Database Host (e.g., `dpg-xxxx-a`) from your Render PostgreSQL details page |
| `DB_PORT` | `5432` |
| `DB_NAME` | `result_db` |
| `DB_USER` | `result_user` |
| `DB_PASSWORD` | Password from your Render PostgreSQL details page |
| `SESSION_COOKIE_SECURE` | `true` |

### Step 3: Deploy
Click **Create Web Service** at the bottom of the page. Render will pull your repo, build the Java container using your `Dockerfile`, and deploy your web app!

---

## Default Administrator Credentials

Upon first startup, `DatabaseSeeder` automatically creates the initial admin account:

- **Username**: `admin`
- **Password**: `admin123`
