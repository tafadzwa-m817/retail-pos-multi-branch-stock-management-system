package zw.co.july28.retail.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import zw.co.july28.retail.entity.CreditAccount;
import zw.co.july28.retail.entity.CreditTransaction;
import zw.co.july28.retail.entity.Customer;
import zw.co.july28.retail.enums.CreditTransactionType;
import zw.co.july28.retail.exception.BadRequestException;
import zw.co.july28.retail.exception.ResourceNotFoundException;
import zw.co.july28.retail.repository.CreditAccountRepository;
import zw.co.july28.retail.repository.CreditTransactionRepository;
import zw.co.july28.retail.repository.CustomerRepository;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CreditAccountService {

    private final CreditAccountRepository accountRepository;
    private final CreditTransactionRepository transactionRepository;
    private final CustomerRepository customerRepository;

    public List<CreditAccount> getAll() { return accountRepository.findAll(); }

    public CreditAccount getById(Long id) {
        return accountRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("CreditAccount", id));
    }

    public CreditAccount getByCustomer(Long customerId) {
        return accountRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("No credit account for customer ID: " + customerId));
    }

    @Transactional
    public CreditAccount openAccount(Long customerId, BigDecimal creditLimit) {
        if (accountRepository.findByCustomerId(customerId).isPresent()) {
            throw new BadRequestException("Customer already has a credit account");
        }
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", customerId));
        return accountRepository.save(CreditAccount.builder()
                .customer(customer).creditLimit(creditLimit).currentBalance(BigDecimal.ZERO).active(true).build());
    }

    @Transactional
    public CreditAccount updateLimit(Long id, BigDecimal newLimit) {
        CreditAccount account = getById(id);
        account.setCreditLimit(newLimit);
        return accountRepository.save(account);
    }

    @Transactional
    public CreditTransaction debitAccount(Long customerId, BigDecimal amount, Long saleId) {
        CreditAccount account = accountRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new BadRequestException("No credit account found for customer"));
        if (!account.isActive()) throw new BadRequestException("Credit account is not active");
        if (account.availableCredit().compareTo(amount) < 0) {
            throw new BadRequestException("Insufficient credit: limit $" + account.getCreditLimit()
                    + ", owed $" + account.getCurrentBalance() + ", requested $" + amount);
        }
        account.setCurrentBalance(account.getCurrentBalance().add(amount));
        accountRepository.save(account);

        return transactionRepository.save(CreditTransaction.builder()
                .account(account).amount(amount).type(CreditTransactionType.DEBIT)
                .saleId(saleId).notes("Sale #" + saleId).build());
    }

    @Transactional
    public CreditTransaction recordRepayment(Long id, BigDecimal amount, String notes) {
        CreditAccount account = getById(id);
        if (amount.compareTo(account.getCurrentBalance()) > 0) {
            throw new BadRequestException("Repayment $" + amount + " exceeds balance owed $" + account.getCurrentBalance());
        }
        account.setCurrentBalance(account.getCurrentBalance().subtract(amount));
        accountRepository.save(account);

        return transactionRepository.save(CreditTransaction.builder()
                .account(account).amount(amount).type(CreditTransactionType.REPAYMENT)
                .notes(notes).build());
    }

    @Transactional
    public void closeAccount(Long id) {
        CreditAccount account = getById(id);
        if (account.getCurrentBalance().compareTo(BigDecimal.ZERO) > 0) {
            throw new BadRequestException("Cannot close account with outstanding balance: $" + account.getCurrentBalance());
        }
        account.setActive(false);
        accountRepository.save(account);
    }

    public List<CreditTransaction> getTransactions(Long accountId) {
        return transactionRepository.findByAccountIdOrderByCreatedAtDesc(accountId);
    }
}
