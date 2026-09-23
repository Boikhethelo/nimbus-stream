# Nimbus Stream

A self-hosted, cloud-native media streaming engine built with Spring Boot, SQLite,
and a swappable storage backend (Amazon S3 or Google Drive).

## Architecture

- **Storage** — `StorageService` interface, implemented by `S3StorageService` (default)
  and `GoogleDriveStorageService`. Select via `storage.provider` in `application.properties`.
  Every consumer (`MediaController`, `MetadataSyncService`, `VideoStreamingService`) depends
  only on the interface, never on a concrete provider, so the backend can be swapped without
  touching any calling code.
- **Streaming** — `VideoStreamingService` handles HTTP range/chunking logic (1MB chunks),
  kept separate from the controller and from storage access. Video bytes are read from the
  active storage provider and proxied through the Spring Boot app to the client — there is
  no direct-from-bucket delivery in this version, which keeps the streaming path identical
  regardless of which provider is active.
- **Auth** — IAM role (S3, resolved automatically on EC2 via the AWS SDK's default
  credential chain) or a service account (Drive). No interactive browser login, no static
  credentials committed to the repo.
- **Local cache** — SQLite, synced from the active storage provider via `MetadataSyncService`
  into the `video_metadata` table. The cache is what `VideoLibraryService` and the controller
  actually read from; a video must be synced before it can be streamed or searched.
- **Error handling** — `GlobalExceptionHandler` translates `VideoNotFoundException` to 404,
  `StorageException` to 502, and anything unanticipated to a generic 500, using RFC 7807
  `ProblemDetail` responses.

## Switching storage providers

Set `storage.provider` in `application.properties` (or as an environment variable override)
to either:

- `s3` — uses `AwsConfig` + `S3StorageService`. Requires `aws.region` and `aws.s3.bucket`
  (see below).
- `google-drive` — uses `GoogleDriveConfig` + `GoogleDriveStorageService`. Requires a service
  account key at classpath resource `/credentials.json`.

## Required configuration

| Property | Provider | Source |
|---|---|---|
| `storage.provider` | both | `application.properties` |
| `aws.region` | s3 | `application.properties` / `AWS_REGION` env var |
| `aws.s3.bucket` | s3 | `application.properties` / `AWS_S3_BUCKET` env var |
| `application.name` | google-drive | `application.properties` |
| service account key | google-drive | classpath `/credentials.json` |

No AWS access keys or Drive credentials are ever read from `application.properties` directly —
S3 auth resolves through the IAM role attached to the EC2 instance (or a local AWS CLI profile
during development), and Drive auth resolves through the service account file.

## API

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/videos` | List every video in the local cache |
| `GET` | `/api/videos/search?title=` | Search cached videos by title (case-insensitive, partial match) |
| `GET` | `/api/videos/stream/{fileId}` | Stream a video, honoring `Range` headers for seeking |
| `POST` | `/api/videos/sync?folderId=` | Re-sync the local cache from the active storage provider (S3 prefix or Drive folder ID) |

## AWS Free Tier boundaries

This project is designed to run entirely inside the 12-month AWS Free Tier, using only S3 and
EC2 — no RDS, ECS/Fargate, or CloudFront.

- **S3**: 5GB total storage, 20,000 GET requests/month, 2,000 PUT requests/month.
- **EC2**: 750 hours/month of `t2.micro` or `t3.micro` (shared across all EC2 usage on the
  account, not per-instance).


```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": ["s3:GetObject", "s3:ListBucket"],
      "Resource": [
        "arn:aws:s3:::YOUR-BUCKET-NAME",
        "arn:aws:s3:::YOUR-BUCKET-NAME/*"
      ]
    }
  ]
}
```

## Known issues

- **Streaming fetches the full object on every chunk request.** `S3StorageService.getVideoStream`
  (and its Google Drive equivalent) issues a full, unranged `GetObject`/`files.get` request every
  time `VideoStreamingService` asks for a chunk. The 1MB range that's actually needed is sliced
  out *locally* by Spring's `ResourceRegionHttpMessageConverter` after the whole file has already
  been downloaded — so the byte savings a "Range" request is supposed to provide never reach the
  network call to the storage provider.

  In practice this is invisible for the first ~15-20 seconds of playback: the browser aggressively
  pre-buffers on load, and that initial burst of requests completes fast enough (S3's first-byte
  latency is low) that nothing looks wrong. Once the pre-buffer is consumed, though, each
  further chunk request is *also* pulling the entire file, and on a burstable `t2`/`t3.micro`
  instance the repeated full-object transfers burn through the instance's network burst credit
  pool — once that's exhausted, throttled bandwidth turns what should be sub-second chunk
  requests into visible stalls.


## Testing

Unit tests cover all business logic (streaming range math, cache sync, mapping, exception
handling, controller delegation) using JUnit 5, Mockito, and AssertJ, with no Spring context
required. Run with:

```bash
mvn clean test
```

WTC-2RGGX6YK
