package com.groom.customer.configuration.jpa

enum class DataSourceType {
    MASTER,
    REPLICA,
    ;

    companion object {
        fun isReadOnlyTransaction(txReadOnly: Boolean): DataSourceType =
            if (txReadOnly) {
                REPLICA
            } else {
                MASTER
            }
    }
}
