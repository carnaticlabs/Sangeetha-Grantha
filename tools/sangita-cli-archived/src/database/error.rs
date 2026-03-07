use std::time::Duration;
use thiserror::Error;

#[derive(Debug, Error)]
pub enum DatabaseError {
    #[error("Connection failure: {0}")]
    Connection(String),

    #[error("Migration failure: {0}")]
    Migration(String),

    #[error("Timeout while {operation} after {duration:?}")]
    Timeout {
        operation: String,
        duration: Duration,
    },
}
