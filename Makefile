#Configurable variables
PORT ?= 8080
HOST ?= localhost
FOLDER_ID ?= test/
FILE_ID ?= test/test-film.mp4
TITLE ?= film
OUT_FILE ?= chunk.mp4

LOCAL_BASE := http://localhost:$(PORT)/api/videos
EC2_BASE := http://$(HOST):$(PORT)/api/videos

.PHONY: help run sync list search stream stream-download stream-headers \
        api play clean-cache \
        ec2-sync ec2-list ec2-search ec2-stream ec2-stream-download ec2-stream-headers ec2-api

 #help:
 help:
 	@echo "Local:"



 # Local: run the app
 run:
 	mvn spring-boot:run

 # Local: populate cache
 sync:
 	curl -X POST "$(LOCAL_BASE)/sync?folderId=$(FOLDER_ID)"

 # Local: API calls
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

 #Local: playback page
 play:
 	xdg-open "http://localhost:$(PORT)/player.html"

 #Local: Housekeeping
 clean-cache:
 	rm -f stream_cache.db

 clean:
 	mvn clean


