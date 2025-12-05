package com.example.stora.navigation
object Routes {
    const val INVENTORY_SCREEN = "inventory"

    const val HOME_SCREEN = "home"
    const val DETAIL_SCREEN = "detail/{itemId}"
    const val ADD_ITEM_SCREEN = "add_item"
    const val EDIT_ITEM_SCREEN = "edit_item/{itemId}"

    const val AUTH_SCREEN = "auth"
    const val LOANS_SCREEN = "loans?showDeleteSnackbar={showDeleteSnackbar}"
    const val NEW_LOAN_SCREEN = "new_loan"
    
    fun loansScreen(showDeleteSnackbar: Boolean = false) = 
        "loans?showDeleteSnackbar=$showDeleteSnackbar"
    const val LOAN_FORM_SCREEN = "loan_form/{selectedItems}"
    const val DETAIL_LOAN_SCREEN = "detail_loan/{loanId}"
    const val DETAIL_LOAN_HISTORY_SCREEN = "detail_loan_history/{loanId}"
    const val PROFILE_SCREEN = "profile"
    const val EDIT_PROFILE_SCREEN = "edit_profile"
    const val SETTING_SCREEN = "setting"

    fun detailScreen(itemId: String) = "detail/$itemId"
    fun editItemScreen(itemId: String) = "edit_item/$itemId"
    fun loanFormScreen(selectedItems: String) = "loan_form/$selectedItems"
    fun detailLoanScreen(loanId: Int) = "detail_loan/$loanId"
    fun detailLoanHistoryScreen(loanId: Int) = "detail_loan_history/$loanId"
}