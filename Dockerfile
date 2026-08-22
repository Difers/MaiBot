# Runtime image
FROM python:3.13-slim
COPY --from=ghcr.io/astral-sh/uv:latest /uv /uvx /bin/

# Working directory
WORKDIR /MaiMBot

ENV MAIBOT_LEGACY_0X_UPGRADE_CONFIRMED=1
ENV PATH="/MaiMBot/.venv/bin:${PATH}"

# 国内镜像源
ENV UV_INDEX_URL=https://mirrors.aliyun.com/pypi/simple/
ENV PLAYWRIGHT_DOWNLOAD_HOST=https://npmmirror.com/mirrors/playwright/

# 替换 apt 源为阿里云
RUN sed -i 's/deb.debian.org/mirrors.aliyun.com/g' /etc/apt/sources.list.d/debian.sources \
    && apt-get update \
    && apt-get install -y --no-install-recommends git \
    && rm -rf /var/lib/apt/lists/*

# Copy dependency metadata
COPY pyproject.toml uv.lock ./

# Install runtime dependencies
RUN uv sync --frozen --no-dev --no-install-project

# Install system libraries required by Playwright Chromium. The browser binary
# itself is downloaded lazily into the configured data directory at runtime.
RUN python -m playwright install-deps chromium \
    && rm -rf /var/lib/apt/lists/*

# Copy project source
COPY . .

# 拉取 NapCat 适配器插件（不会被 git 跟踪，需在构建时 clone）
RUN git clone --depth 1 --branch main \
    https://ghfast.top/github.com/Mai-with-u/MaiBot-Napcat-Adapter.git \
    plugin-templates/MaiBot-Napcat-Adapter

RUN chmod +x docker-entrypoint.sh

EXPOSE 8000 8001

ENTRYPOINT [ "./docker-entrypoint.sh" ]
