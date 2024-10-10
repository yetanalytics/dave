# Use the official Clojure image for building
FROM clojure:temurin-23-tools-deps-bullseye-slim

# Set working directory
WORKDIR /app

# Copy only deps.edn (or project.clj) first to cache dependencies layer
COPY deps.edn ./

# Install dependencies. This step will be cached unless deps.edn changes
RUN clojure -P

# Now copy the rest of the application source code
COPY . .

# Install Node.js and npm (if needed for Figwheel or other tasks)
RUN apt-get update && apt-get install -y nodejs npm

# Build a static html/js page
RUN clojure -M:build-prod

# Build the SASS file
RUN clojure -M:build-sass

# Expose the application port
EXPOSE 9500

# Run Figwheel with the options from :main-opts
CMD ["clojure", "-M:build-prod-server"]


