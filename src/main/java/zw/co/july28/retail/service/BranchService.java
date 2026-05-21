package zw.co.july28.retail.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import zw.co.july28.retail.dto.request.BranchRequest;
import zw.co.july28.retail.dto.response.BranchResponse;
import zw.co.july28.retail.entity.Branch;
import zw.co.july28.retail.exception.BadRequestException;
import zw.co.july28.retail.exception.ResourceNotFoundException;
import zw.co.july28.retail.repository.BranchRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BranchService {

    private final BranchRepository branchRepository;

    public List<BranchResponse> getAllBranches() {
        return branchRepository.findAll().stream()
                .map(BranchResponse::from)
                .collect(Collectors.toList());
    }

    public List<BranchResponse> getActiveBranches() {
        return branchRepository.findByActiveTrue().stream()
                .map(BranchResponse::from)
                .collect(Collectors.toList());
    }

    public BranchResponse getBranch(Long id) {
        return BranchResponse.from(findById(id));
    }

    public BranchResponse createBranch(BranchRequest request) {
        if (branchRepository.existsByName(request.getName())) {
            throw new BadRequestException("Branch name already exists: " + request.getName());
        }
        Branch branch = Branch.builder()
                .name(request.getName())
                .address(request.getAddress())
                .phone(request.getPhone())
                .email(request.getEmail())
                .active(request.isActive())
                .build();
        return BranchResponse.from(branchRepository.save(branch));
    }

    public BranchResponse updateBranch(Long id, BranchRequest request) {
        Branch branch = findById(id);
        if (!branch.getName().equals(request.getName()) && branchRepository.existsByName(request.getName())) {
            throw new BadRequestException("Branch name already exists: " + request.getName());
        }
        branch.setName(request.getName());
        branch.setAddress(request.getAddress());
        branch.setPhone(request.getPhone());
        branch.setEmail(request.getEmail());
        branch.setActive(request.isActive());
        return BranchResponse.from(branchRepository.save(branch));
    }

    public void deleteBranch(Long id) {
        Branch branch = findById(id);
        branch.setActive(false);
        branchRepository.save(branch);
    }

    private Branch findById(Long id) {
        return branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Branch", id));
    }
}
