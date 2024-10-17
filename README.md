# Coni

Coni is an automated testing tool for database connectors. It reads the pre-defined connector state model and then generates semantically correct test cases based on the model. Coni utilizes differential testing to detect bugs in database connectors. Specifically, Coni compares whether the results from MariaDB Connector/J and AWS MySQL JDBC are consistent with those from MySQL Connector/J, and whether the results from PG JDBC are consistent with PG JDBC NG.

Coni-supported connectors:

* MySQL Connector/J
* MariaDB Connector/J
* AWS MySQL JDBC
* PG JDBC
* PG JDBC NG

# Getting Started

Requirements:

* Java 17 or above
* Maven (`sudo apt install maven` on Ubuntu)
* Docker (`sudo apt install docker-ce` on Ubuntu)
* The corresponding DBMSs for testing database connectors (i.e., MySQL for MySQL Connector/J)

The default run time is one hour. Running the following commands to start Coni:

```bash
cd Coni
docker compose up
mvn clean package
mvn dependency:copy-dependencies
java -classpath "target/Coni-1.0-SNAPSHOT.jar:target/dependency/*" Main ./property/mysql_cp.properties
```

Coni prints progress information every minute. 
The inconsistent behaviors detected during testing are categorized and output to the `/out` directory. The argument passed to `Main` is the config file path.

# Additional Documentation

[Coni JDBC State Model](https://docs.google.com/spreadsheets/d/1gTIH_F9nV7seuzc0GppZY9lxoYksaAbeF2RQoPpg8CQ/edit?usp=sharing)

# Citation

If you are interested in this work, please feel free to leave a star or cite us through:

```
@inproceedings{deng2025coni,
  title={CONI: Detecting Database Connector Bugs via State-Aware Test Case Generation},
  author={Deng, Wenqian and Liang, Jie and Wu, Zhiyong and Fu, Jigzhou and Wang, Mingzhe and Jiang, Yu},
  booktitle={Proceedings of the IEEE/ACM 47th International Conference on Software Engineering},
  pages={1--12},
  year={2025}
}
```
