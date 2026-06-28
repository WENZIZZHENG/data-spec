package com.dataspec.contract;

import com.dataspec.common.result.R;
import com.dataspec.contract.controller.SchemaRegistryController;
import com.dataspec.contract.model.SchemaContract;
import com.dataspec.contract.model.SchemaRegistryCatalog;
import com.dataspec.contract.service.SchemaRegistryService;
import com.dataspec.contract.service.impl.SchemaRegistryServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchemaRegistryControllerTest {

    @Test
    void listContractsDelegatesToService() {
        RecordingSchemaRegistryService service = new RecordingSchemaRegistryService();
        SchemaRegistryController controller = new SchemaRegistryController(service);

        R<SchemaRegistryCatalog> response = controller.listContracts();

        assertEquals(200, response.getCode());
        assertTrue(service.catalogCalled);
        assertEquals("dataspec-schema-registry", response.getData().getKind());
    }

    @Test
    void getContractDelegatesToService() {
        RecordingSchemaRegistryService service = new RecordingSchemaRegistryService();
        SchemaRegistryController controller = new SchemaRegistryController(service);

        R<SchemaContract> response = controller.getContract("field");

        assertEquals(200, response.getCode());
        assertEquals("field", service.lastContractId);
        assertEquals("field", response.getData().getContractId());
    }

    private static class RecordingSchemaRegistryService implements SchemaRegistryService {
        private final SchemaRegistryService delegate = new SchemaRegistryServiceImpl();
        private boolean catalogCalled;
        private String lastContractId;

        @Override
        public SchemaRegistryCatalog getCatalog() {
            catalogCalled = true;
            return delegate.getCatalog();
        }

        @Override
        public SchemaContract getContract(String contractId) {
            lastContractId = contractId;
            return delegate.getContract(contractId);
        }

        @Override
        public List<String> requiredContractIds() {
            return delegate.requiredContractIds();
        }

        @Override
        public Map<String, Object> manifestSummary() {
            return delegate.manifestSummary();
        }
    }
}
