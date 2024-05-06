# Copy Code From Existing Project
# rename Module
# rename project
    open main.java
    select the package name and rename it
    Make sure porm.xml copied for external libray and .idea aswell
# Create Artifacts for exe 
# for database timer
    SET GLOBAL event_scheduler = ON;
# To know active connections of HikariPool 
    HelperConfiguration.hikariDataSource.getHikariPoolMXBean().getActiveConnections()