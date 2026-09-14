alter table body_scan
    drop column type_of_find;

delete
from reference_data_code
where domain = 'TYPE_OF_FIND';

delete
from reference_data_domain
where code = 'TYPE_OF_FIND'
