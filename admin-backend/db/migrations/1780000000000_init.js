exports.up = (pgm) => {
  pgm.createTable('users', {
    id: { type: 'varchar(128)', primaryKey: true },
    email: { type: 'varchar(255)', notNull: true, unique: true },
    role: { type: 'varchar(64)', notNull: true },
    status: { type: 'varchar(64)', notNull: true },
    created_at: { type: 'timestamptz', notNull: true, default: pgm.func('current_timestamp') },
    last_login_at: { type: 'timestamptz' }
  });

  pgm.createTable('admin_actions', {
    id: { type: 'varchar(128)', primaryKey: true },
    type: { type: 'varchar(64)', notNull: true },
    target_user_id: { type: 'varchar(128)' },
    performed_by: { type: 'varchar(128)', notNull: true },
    description: { type: 'text', notNull: true },
    timestamp: { type: 'timestamptz', notNull: true, default: pgm.func('current_timestamp') }
  });

  pgm.createTable('announcements', {
    id: { type: 'varchar(128)', primaryKey: true },
    title: { type: 'varchar(255)', notNull: true },
    content: { type: 'text', notNull: true },
    category: { type: 'varchar(64)', notNull: true },
    author_id: { type: 'varchar(128)', notNull: true },
    status: { type: 'varchar(64)', notNull: true },
    timestamp: { type: 'timestamptz', notNull: true, default: pgm.func('current_timestamp') }
  });
};

exports.down = (pgm) => {
  pgm.dropTable('announcements');
  pgm.dropTable('admin_actions');
  pgm.dropTable('users');
};
