FROM node:20-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
# Inlined into the browser bundle at build time. Empty = relative URLs (same-origin via the
# Next.js proxy or nginx).
ARG NEXT_PUBLIC_API_URL=""
# Direct backend URL for SSE and Google OAuth2 (see src/lib/config.ts); empty = use
# NEXT_PUBLIC_API_URL.
ARG NEXT_PUBLIC_DIRECT_BACKEND_URL=""
ARG NEXT_PUBLIC_RAZORPAY_KEY_ID=""
# Server-side backend URL used by the rewrites in next.config.ts (baked in at build time).
ARG BACKEND_URL=""
ENV NEXT_PUBLIC_API_URL=${NEXT_PUBLIC_API_URL}     NEXT_PUBLIC_DIRECT_BACKEND_URL=${NEXT_PUBLIC_DIRECT_BACKEND_URL}     NEXT_PUBLIC_RAZORPAY_KEY_ID=${NEXT_PUBLIC_RAZORPAY_KEY_ID}     BACKEND_URL=${BACKEND_URL}
RUN npm run build

FROM node:20-alpine AS run
WORKDIR /app
ENV NODE_ENV=production     HOSTNAME=0.0.0.0     PORT=3000
RUN addgroup -S app && adduser -S app -G app
COPY --from=build --chown=app:app /app/.next/standalone ./
COPY --from=build --chown=app:app /app/.next/static ./.next/static
COPY --from=build --chown=app:app /app/public ./public
USER app
# Hosts like Render inject $PORT; the standalone server reads it.
EXPOSE 3000
CMD ["node", "server.js"]
