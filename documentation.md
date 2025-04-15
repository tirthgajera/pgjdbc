# Project 3: PostgreSQL JDBC Driver
PostgreSQL Java Database Connectivity Driver is a driver that helps Java apps talk to PostgreSQL databases.

**Original repository:** https://github.com/pgjdbc/pgjdbc

# Overview of PGJDBC
PGJDBC is an open source Java library that enables Java programs to connect, query and update PostgreSQL databases using the standard SQL.

**Why we chose it?**
It is a tool that is used widely in real world productions of Java applications.

This open source library is maintained by The PostgreSQL Global Development Group and contributors from the Java/PostgreSQL community.

# Breadth-Wise analysis
**1. JDBC specifications:** Supports JDBC 4.2 and above.

**2. Secure connection** using SSL and TLS.

**3. High level architecture of PGJDBC:** It has a modular architecture as follows:
- **Connection Management:** Handles establishing and managing database connections.
- **Query Execution:** Parses and executes SQL queries.
- **Result Handling:** Processes results returned from the database.
- **Type Mapping:** Maps PostgreSQL data types to Java types.
- **Protocol Interface:** Manages communication using PostgreSQL's native protocol.

**4. Files and folders:**
```plaintext
pgjdbc/
├── .github/                # GitHub-specific configurations and workflows
├── benchmarks/             # Performance benchmarking tools
├── build-logic-commons/    # Shared build logic scripts
├── build-logic/            # Build configurations and scripts
├── certdir/                # SSL certificate files for testing
├── config/                 # Configuration files
├── docker/                 # Docker configurations for testing environments
├── docs/                   # Project documentation
├── gradle/                 # Gradle wrapper files
├── packaging/              # Packaging scripts for distributions
├── pgjdbc-osgi-test/       # OSGi-specific tests
├── pgjdbc/                 # Main source code
│   ├── src/
│   │   ├── main/
│   │   │   └── java/
│   │   │       └── org/
│   │   │           └── postgresql/    # Core driver implementation
│   │   └── test/
│   │       └── java/
│   │           └── org/
│   │               └── postgresql/    # Test cases
├── test-anorm-sbt/         # Tests for Anorm (Scala) integration
├── test-gss/               # Tests for GSSAPI (Kerberos) authentication
├── .editorconfig           # Editor configuration
├── .gitignore              # Git ignore rules
├── build.gradle.kts        # Gradle build script
├── settings.gradle.kts     # Gradle settings
├── README.md               # Project overview
```

# Depth Wise analysis
## Bugfix Case Study: CHAR_OCTET_LENGTH on Non-Character Types

While working on the project, we investigated an inconsistency reported in [Issue #3465](https://github.com/pgjdbc/pgjdbc/issues/3465):

> **CHAR_OCTET_LENGTH** was returning a hardcoded value (e.g., 10) even for `integer[]` type columns, which contradicts JDBC specifications.

### 🛠️ Our Fix
We modified the implementation of `DatabaseMetaData#getColumns()` in `PgDatabaseMetaData.java` to return:
- The actual number of bytes **only for character and binary types** (e.g., `CHAR`, `VARCHAR`, `BYTEA`, `CLOB`, `BLOB`, etc.)
- `null` for all **non-character and non-binary types** (e.g., `integer[]`, `int`, `float`, etc.)

This involved checking `java.sql.Types` using `sqlType`, and also PostgreSQL type names using `pgType`. This is especially useful for PostgreSQL-specific types like `bytea`.

### ✅ Final logic used:
```java
if (sqlType == Types.CHAR ||
    sqlType == Types.VARCHAR ||
    sqlType == Types.LONGVARCHAR ||
    sqlType == Types.NCHAR ||
    sqlType == Types.NVARCHAR ||
    sqlType == Types.LONGNVARCHAR ||
    sqlType == Types.BINARY ||
    sqlType == Types.VARBINARY ||
    sqlType == Types.LONGVARBINARY ||
    sqlType == Types.BLOB ||
    sqlType == Types.CLOB ||
    "bytea".equals(pgType)) {
    tuple[15] = connection.encodeString(Integer.toString(columnSize));
} else {
    tuple[15] = null;
}
```

### 🧪 Testing Strategy
A comprehensive helper-based test was added inside `DatabaseMetaDataTest.java`:
- It creates a table `char_octet_test` with columns of different types: `varchar`, `char`, `bytea`, `integer[]`, etc.
- The test checks:
    - For character and binary types → `CHAR_OCTET_LENGTH` returns column size
    - For non-character types → `CHAR_OCTET_LENGTH` is `null`
- Additional PostgreSQL types like `clob`, `blob`, and their behavior are also verified

### ⚙️ PostgreSQL Setup
We tested the changes using a local PostgreSQL setup accessible through:
- **Username:** `postgres`
- **Password:** `postgres`
- **Port:** `5432`
- **Database URL:** `jdbc:postgresql://localhost:5432/test`

Use PGAdmin 4 or psql to create and view the test table:
```sql
CREATE TABLE char_octet_test (
  intarray integer[],
  mytext varchar(100),
  mychar char(10),
  mybinary bytea
);
```

### 💡 Trade-offs and Analysis
- We used a mix of `sqlType` from JDBC and PostgreSQL-specific `pgType` checks to ensure broader compatibility.
- PostgreSQL treats types like `bytea` differently, so manual string-based pgType checks were needed.
- This fix improves JDBC standard compliance without affecting existing binary-safe behavior.

### 📂 Files Modified
- `PgDatabaseMetaData.java` → Main fix in `getColumns()` method.
- `DatabaseMetaDataTest.java` → Added helper-based tests.

# Fix for Bug #3465 in pgJDBC
This repository contains our fix for [Bug #3465](https://github.com/pgjdbc/pgjdbc/issues/3465) in the `pgjdbc` project — the PostgreSQL JDBC Driver.

### 🔍 Bug Summary
CHAR_OCTET_LENGTH for non-character types was incorrectly returning a value instead of `null`, which goes against JDBC specifications.

### 🧪 Validation
We validated our fix using a custom test table containing all edge case column types and asserted the correctness of values using `DatabaseMetaData.getColumns()`.

# ✅ Pull Requests
We have fixed the issue and raised pull requests to the author (Dave Cramer: https://github.com/davecramer) and it has been accepted.
- PRs are merged in our fork: https://github.com/tirthgajera/pgjdbc/pulls?q=is%3Apr+is%3Aclosed
