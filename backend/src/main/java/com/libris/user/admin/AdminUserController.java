package com.libris.user.admin;

import com.libris.shared.web.PageResponse;
import com.libris.user.admin.dto.UserSummaryResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Account administration.
 *
 * <p>Neither route appears in the endpoint list of the statement, but the rules do
 * require them: an ADMIN has to be able to see who is blocked and to lift a block early.
 * Both are documented in the README.
 */
@Tag(name = "Administración", description = "Gestión de cuentas y bloqueos")
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final UserAdminService userAdminService;

    public AdminUserController(UserAdminService userAdminService) {
        this.userAdminService = userAdminService;
    }

    /**
     * @param blocked null lists every account, true only the blocked ones, false only the
     *                ones that can borrow. Backs the "Todos / Activos / Bloqueados" tabs.
     */
    @GetMapping
    public PageResponse<UserSummaryResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean blocked,
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return userAdminService.search(search, blocked, pageable);
    }

    @PutMapping("/{id}/unblock")
    public UserSummaryResponse unblock(@PathVariable Long id) {
        return userAdminService.unblock(id);
    }
}
