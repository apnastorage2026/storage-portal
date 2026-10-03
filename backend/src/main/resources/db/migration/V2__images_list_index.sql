-- List loads one user's images, newest first. This index serves that order.
CREATE INDEX IF NOT EXISTS images_user_uploaded_at_idx
    ON images (user_id, uploaded_at DESC, id DESC);
