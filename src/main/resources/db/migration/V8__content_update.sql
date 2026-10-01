update reference_data_code
set description='Intelligence-led cohort'
where domain = 'JUSTIFICATION'
  and code = 'INTELLIGENCE';

update reference_data_code
set description='Negative'
where domain = 'OUTCOME'
  and code = 'NEGATIVE';
update reference_data_code
set description='Positive'
where domain = 'OUTCOME'
  and code = 'POSITIVE';
