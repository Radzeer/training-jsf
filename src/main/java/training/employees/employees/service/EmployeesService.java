package training.employees.employees.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import training.employees.employees.dto.*;
import training.employees.employees.jmsgateway.EmployeeGateway;
import training.employees.employees.jmsgateway.EventStoreGateway;
import training.employees.employees.repository.AddressesRepository;
import training.employees.employees.repository.EmployeeNotFoundException;
import training.employees.employees.repository.EmployeesRepository;


import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class EmployeesService {

    private EmployeesRepository repository;
    private AddressesRepository addressesRepository;

    private EmployeeMapper employeeMapper;

    private EventStoreGateway eventStoreGateway;
    private EmployeeGateway employeeGateway;

    private MeterRegistry meterRegistry;

    @PostConstruct
    public void initCounter() {
        Counter.builder("employees.created")
                .baseUnit("employees")
                .description("Number of created employees")
                .register(meterRegistry);

    }

    public List<EmployeeDto> listEmployees(Optional<String> prefix) {
        if (prefix.isEmpty()) {
            return repository.findEmployeeDtos();
        }
        else {
            return repository.findEmployeeDtosByNameLike(prefix.get().toLowerCase() + "%");
        }
    }

    public EmployeeDetailsDto findEmployeeById(long id) {
      return employeeMapper.toDto(repository.findById(id)
                .orElseThrow(()-> new EmployeeNotFoundException("Employee not found with id: "+id)));
    }

    public EmployeeDetailsDto createEmployee(CreateEmployeeCommand command) {
        var employee = employeeMapper.toEntity(command);
        repository.save(employee);
        eventStoreGateway.sendEvent(String.format("Employee has been created: %s",command.getName()));
        employeeGateway.sendEvent(String.format("Employee has been created: %s", command.getName()));

        meterRegistry.counter("employees.created").increment();

        return employeeMapper.toDto(employee);
    }

    @Transactional
    public EmployeeDetailsDto updateEmployee(long id, UpdateEmployeeCommand command) {
        var employee = repository.findById(id).orElseThrow(()-> new EmployeeNotFoundException("Employee not found with id: "+id));
            employee.setYearOfBirth(command.getYearOfBirth());
        return employeeMapper.toDto(employee);
    }

    public void deleteEmployee(long id) {
        repository.deleteById(id);
    }
    @Transactional
    public AddressDto createAddress(long employeeId,CreateAddressCommand command){
        var address = employeeMapper.toEntity(command);
        addressesRepository.save(address);
        var employee = repository.findById(employeeId)
                .orElseThrow(() -> new EmployeeNotFoundException("Employee not found with id: "+employeeId));
        employee.addAddress(address);
        return employeeMapper.toDto(address);
    }

    public List<AddressDto> listAdresses(long employeeId){
        var employee = repository.findEmployeeWithAdresses(employeeId)
                .orElseThrow(() -> new EmployeeNotFoundException("Employee not found with id: "+employeeId));
        return employeeMapper.toAddressDto(employee.getAddresses());
    }
}
