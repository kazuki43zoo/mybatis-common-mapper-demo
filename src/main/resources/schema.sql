CREATE TABLE todo (
  id IDENTITY PRIMARY KEY
  ,title TEXT NOT NULL
  ,finished BOOLEAN NOT NULL
  ,created_at TIMESTAMP NOT NULL
  ,deadline DATE
  ,finished_at TIMESTAMP
);

