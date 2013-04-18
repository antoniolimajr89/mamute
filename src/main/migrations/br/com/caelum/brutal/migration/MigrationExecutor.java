package br.com.caelum.brutal.migration;

import java.math.BigInteger;
import java.util.List;

import org.hibernate.SessionFactory;
import org.hibernate.classic.Session;

import br.com.caelum.brutal.providers.SessionFactoryCreator;
import br.com.caelum.vraptor.ioc.ApplicationScoped;
import br.com.caelum.vraptor.ioc.Component;

@Component
@ApplicationScoped
public class MigrationExecutor {

	private final SessionFactory sf;
	private int currentMigration = -1;
	private Session session;
	private final SessionFactoryCreator creator;

	public MigrationExecutor(SessionFactory sf, SessionFactoryCreator creator) {
		this.sf = sf;
		this.creator = creator;
	}

	public void begin() {
		session = sf.openSession();
		session.beginTransaction();
	}

	public void end() {
		session.getTransaction().commit();
	}

	public void rollback() {
		session.getTransaction().rollback();
	}
	
	public void rollback(SimpleMigration m) {
		if (m.hasDown()) {
			executeQueries(m.down());
		}
		rollback();
	}

	public void insertNewMigration(int lastMigration) {
		session.createSQLQuery("insert into brutalmigration(number) values(:last)").setInteger("last", lastMigration).executeUpdate();
	}

	public int currentMigration() {
		if (currentMigration == -1) {
			currentMigration = (int) session.createSQLQuery("select number from brutalmigration order by number desc limit 1").uniqueResult();
		}
		return currentMigration;
	}

	private void executeQueries(List<RawSQLMigration> queries) {
		for (RawSQLMigration rawSQLMigration : queries) {
			String sql = rawSQLMigration.up();
			session.createSQLQuery(sql).executeUpdate();
		}
	}

	public void run(SimpleMigration m) {
		executeQueries(m.up());
	}

	public void prepareTables() {
		session.createSQLQuery("create table if not exists brutalmigration (number int(11))").executeUpdate();
		BigInteger result = (BigInteger) session.createSQLQuery("select count(*) from brutalmigration").uniqueResult();
		if (result.equals(BigInteger.ZERO)) {
			creator.createFromScratch();
			session.createSQLQuery("insert into brutalmigration values(0)").executeUpdate();
		}
	}

	public void close() {
		session.close();
	}

}
