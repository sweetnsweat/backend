alter table wallet_transactions
    drop constraint if exists wallet_transactions_tx_type_check;

alter table wallet_transactions
    add constraint wallet_transactions_tx_type_check
    check (tx_type in (
        'quest_reward',
        'competition_reward',
        'purchase',
        'use',
        'refund',
        'admin_adjust',
        'signup_bonus'
    ));
