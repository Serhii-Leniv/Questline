package com.questline.common;

import com.questline.security.CurrentUser;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.util.UUID;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Sets the Postgres GUC {@code app.user_id} to the authenticated user at the start of every
 * transaction, so Row-Level Security policies can isolate tenants at the database level. When there
 * is no authenticated user (background jobs, startup), the GUC is left unset and the policies pass
 * through — so service/job access still works. Uses {@code SET LOCAL}, which resets on commit, so a
 * pooled connection never leaks one user's context to another.
 */
public class RlsTransactionManager extends JpaTransactionManager {

    public RlsTransactionManager(EntityManagerFactory emf) {
        super(emf);
    }

    @Override
    protected void doBegin(Object transaction, TransactionDefinition definition) {
        super.doBegin(transaction, definition);
        UUID userId = CurrentUser.id();
        if (userId == null) {
            return;
        }
        EntityManagerHolder holder =
                (EntityManagerHolder) TransactionSynchronizationManager.getResource(getEntityManagerFactory());
        if (holder == null) {
            return;
        }
        EntityManager em = holder.getEntityManager();
        // userId is a parsed UUID, so its canonical string is safe to inline (SET takes no params).
        em.createNativeQuery("set local app.user_id = '" + userId + "'").executeUpdate();
    }
}
