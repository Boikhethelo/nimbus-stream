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

To stay within these limits:

- Keep test video files small; the entire library must fit inside 5GB.
- Stop the EC2 instance when not actively demoing or testing — 750 hours/month is generous
  but not unlimited if left running continuously alongside other instances.
- Check the AWS Billing & Cost Management dashboard periodically while building, and set a
  budget alert (e.g. at $1) as a tripwire.
- Delete the S3 bucket and terminate the EC2 instance after final submission/grading if the
  project won't keep running.

The IAM role attached to the EC2 instance should be scoped to only what the app needs:

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

## Testing

Unit tests cover all business logic (streaming range math, cache sync, mapping, exception
handling, controller delegation) using JUnit 5, Mockito, and AssertJ, with no Spring context
required. Run with:

```bash
mvn clean test
```

WTC-2RGGX6YK
