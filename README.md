# Nimbus-Stream
A self-hosted, cloud-native media streaming engine built with Spring Boot, SQLite,
and a swappable storage backend (Amazon S3 or Google Drive).

## Architecture

- **Storage** — `StorageService` interface, implemented by `S3StorageService` (default)
  and `GoogleDriveStorageService`. Select via `storage.provider` in `application.properties`.
- **Auth** — IAM role (S3, on EC2) or service account (Drive). No interactive
  browser login, no static credentials committed to the repo.
- **Local cache** — SQLite, synced from the active storage provider via `MetadataSyncService`.
- **Streaming** — `VideoStreamingService` handles HTTP range/chunking logic, kept
  separate from the controller and from storage access.

WTC-2RGGX6YK
