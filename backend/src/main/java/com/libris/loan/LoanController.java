package com.libris.loan;

import com.libris.auth.AuthenticatedUser;
import com.libris.loan.dto.CreateLoanRequest;
import com.libris.loan.dto.LoanResponse;
import com.libris.shared.web.PageResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Préstamos", description = "Registro, consulta y devolución de préstamos")
@RestController
@RequestMapping("/api/loans")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @PostMapping
    public ResponseEntity<LoanResponse> create(
            @Valid @RequestBody CreateLoanRequest request,
            @AuthenticationPrincipal AuthenticatedUser caller) {
        return ResponseEntity.status(HttpStatus.CREATED).body(loanService.create(request, caller));
    }

    @GetMapping("/mine")
    public PageResponse<LoanResponse> mine(
            @AuthenticationPrincipal AuthenticatedUser caller,
            @PageableDefault(size = 20, sort = "loanDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return loanService.findMine(caller, pageable);
    }

    @PutMapping("/{id}/return")
    public LoanResponse returnLoan(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser caller) {
        return loanService.returnLoan(id, caller);
    }
}
