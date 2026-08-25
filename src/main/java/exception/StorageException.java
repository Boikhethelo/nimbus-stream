package exception;
/** Wraps provider-specific failures (S3Exception, IOException) behind a single, storage-agnostic type. */
public class StorageException extends RuntimeException{
    public StorageException(String message, Throwable cause){
        super(message, cause);
    }

}
