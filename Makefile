
# ---- Configurable variables -------------------------------------------------
PORT       ?= 8080
HOST       ?= localhost
FOLDER_ID  ?= test/
FILE_ID    ?= test/test-film.mp4
TITLE      ?= film
RANGE      ?= bytes=0-1048575
OUT_FILE   ?= chunk.mp4

LOCAL_BASE := http://localhost:$(PORT)/api/videos
EC2_BASE   := http://$(HOST):$(PORT)/api/videos

.PHONY: help run sync list search stream stream-download stream-headers \
        api play clean clean-cache \
        ec2-sync ec2-list ec2-search ec2-stream ec2-stream-download ec2-stream-headers ec2-api ec2-play

# ---- Help --------------------------------------------------------------
help:
	@echo "Local:"
	@echo "  make run                Start the app (mvn spring-boot:run)"
	@echo "  make sync               POST /sync to populate the local cache (FOLDER_ID=$(FOLDER_ID))"
	@echo "  make list               GET all cached videos"
	@echo "  make search             GET search by title (TITLE=$(TITLE))"
	@echo "  make stream             Range GET a video chunk to stdout (RANGE=$(RANGE))"
	@echo "  make stream-download    Range GET a video chunk, saved to OUT_FILE=$(OUT_FILE)"
	@echo "  make stream-headers     HEAD-style range request, headers only"
	@echo "  make api                Run sync + list + search + stream-download in sequence"
	@echo "  make play               Open the playback page in your browser"
	@echo "  make clean              mvn clean (build artifacts)"
	@echo "  make clean-cache        Remove the local SQLite cache file (stop the app first)"
	@echo ""
	@echo "EC2 (override HOST=, PORT=$(PORT) as needed):"
	@echo "  make ec2-sync HOST=<ec2-host>"
	@echo "  make ec2-list HOST=<ec2-host>"
	@echo "  make ec2-search HOST=<ec2-host>"
	@echo "  make ec2-stream HOST=<ec2-host>"
	@echo "  make ec2-stream-download HOST=<ec2-host>"
	@echo "  make ec2-stream-headers HOST=<ec2-host>"
	@echo "  make ec2-api HOST=<ec2-host>       Run the full EC2 sequence"
	@echo "  make ec2-play HOST=<ec2-host>      Open the playback page served from EC2"
	@echo ""
	@echo "Variables: PORT, FOLDER_ID, FILE_ID, TITLE, RANGE, OUT_FILE, HOST (EC2 only)"

# ---- Local: run the app ------------------------------------------------
run:
	mvn spring-boot:run

# ---- Local: populate cache ----------------------------------------------
sync:
	curl -X POST "$(LOCAL_BASE)/sync?folderId=$(FOLDER_ID)"

# ---- Local: API calls ---------------------------------------------------
list:
	curl "$(LOCAL_BASE)"

search:
	curl "$(LOCAL_BASE)/search?title=$(TITLE)"

stream:
	curl -H "Range: $(RANGE)" "$(LOCAL_BASE)/stream/$(FILE_ID)"

stream-download:
	curl -H "Range: $(RANGE)" "$(LOCAL_BASE)/stream/$(FILE_ID)" --output $(OUT_FILE)

stream-headers:
	curl -I -H "Range: $(RANGE)" "$(LOCAL_BASE)/stream/$(FILE_ID)"

# Runs the whole local flow in order: populate cache, then exercise each endpoint
api: sync list search stream-download
	@echo "Done - chunk saved to $(OUT_FILE)"

# ---- Local: playback page ------------------------------------------------
# Requires src/main/resources/static/player.html to exist and the app to be running.
play:
	xdg-open "http://localhost:$(PORT)/player.html"

# ---- Local: housekeeping -------------------------------------------------
clean:
	mvn clean

# Stop the app first - HikariCP holds an open file handle while it's running.
clean-cache:
	rm -f stream_cache.db

# ---- EC2: populate cache --------------------------------------------------
ec2-sync:
	curl -X POST "$(EC2_BASE)/sync?folderId=$(FOLDER_ID)"

# ---- EC2: API calls --------------------------------------------------------
ec2-list:
	curl "$(EC2_BASE)"

ec2-search:
	curl "$(EC2_BASE)/search?title=$(TITLE)"

ec2-stream:
	curl -H "Range: $(RANGE)" "$(EC2_BASE)/stream/$(FILE_ID)"

ec2-stream-download:
	curl -H "Range: $(RANGE)" "$(EC2_BASE)/stream/$(FILE_ID)" --output $(OUT_FILE)

ec2-stream-headers:
	curl -I -H "Range: $(RANGE)" "$(EC2_BASE)/stream/$(FILE_ID)"

# Runs the whole EC2 flow in order
ec2-api: ec2-sync ec2-list ec2-search ec2-stream-download
	@echo "Done - chunk saved to $(OUT_FILE)"

# ---- EC2: playback page ----------------------------------------------------
# Assumes the same jar (with src/main/resources/static/player.html baked in)
# is deployed on the EC2 instance - the page's relative src="/api/videos/..."
# resolves against EC2 automatically, no HTML edits needed. Requires HOST=.
ec2-play:
	@if [ "$(HOST)" = "localhost" ]; then \
		echo "Set HOST=<ec2-host-or-ip>, e.g. make ec2-play HOST=13.51.xx.xx"; \
		exit 1; \
	fi
	xdg-open "http://$(HOST):$(PORT)/player.html"

