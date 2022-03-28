CREATE TABLE loanapplicationregister2020
(
    id                                integer                                   NOT NULL,
    lei                               character varying                         NOT NULL,
    uli                               character varying,
    application_date                  character varying,
    loan_type                         integer,
    loan_purpose                      integer,
    preapproval                       integer,
    construction_method               character varying,
    occupancy_type                    integer,
    loan_amount                       numeric,
    action_taken_type                 integer,
    action_taken_date                 integer,
    street                            character varying,
    city                              character varying,
    state                             character varying,
    zip                               character varying,
    county                            character varying,
    tract                             character varying,
    ethnicity_applicant_1             character varying,
    ethnicity_applicant_2             character varying,
    ethnicity_applicant_3             character varying,
    ethnicity_applicant_4             character varying,
    ethnicity_applicant_5             character varying,
    other_hispanic_applicant          character varying,
    ethnicity_co_applicant_1          character varying,
    ethnicity_co_applicant_2          character varying,
    ethnicity_co_applicant_3          character varying,
    ethnicity_co_applicant_4          character varying,
    ethnicity_co_applicant_5          character varying,
    other_hispanic_co_applicant       character varying,
    ethnicity_observed_applicant      integer,
    ethnicity_observed_co_applicant   integer,
    race_applicant_1                  character varying,
    race_applicant_2                  character varying,
    race_applicant_3                  character varying,
    race_applicant_4                  character varying,
    race_applicant_5                  character varying,
    other_native_race_applicant       character varying,
    other_asian_race_applicant        character varying,
    other_pacific_race_applicant      character varying,
    race_co_applicant_1               character varying,
    race_co_applicant_2               character varying,
    race_co_applicant_3               character varying,
    race_co_applicant_4               character varying,
    race_co_applicant_5               character varying,
    other_native_race_co_applicant    character varying,
    other_asian_race_co_applicant     character varying,
    other_pacific_race_co_applicant   character varying,
    race_observed_applicant           integer,
    race_observed_co_applicant        integer,
    sex_applicant                     integer,
    sex_co_applicant                  integer,
    observed_sex_applicant            integer,
    observed_sex_co_applicant         integer,
    age_applicant                     integer,
    age_co_applicant                  integer,
    income                            character varying,
    purchaser_type                    integer,
    rate_spread                       character varying,
    hoepa_status                      integer,
    lien_status                       integer,
    credit_score_applicant            integer,
    credit_score_co_applicant         integer,
    credit_score_type_applicant       integer,
    credit_score_model_applicant      character varying,
    credit_score_type_co_applicant    integer,
    credit_score_model_co_applicant   character varying,
    denial_reason1                    character varying,
    denial_reason2                    character varying,
    denial_reason3                    character varying,
    denial_reason4                    character varying,
    other_denial_reason               character varying,
    total_loan_costs                  character varying,
    total_points                      character varying,
    origination_charges               character varying,
    discount_points                   character varying,
    lender_credits                    character varying,
    interest_rate                     character varying,
    payment_penalty                   character varying,
    debt_to_incode                    character varying,
    loan_value_ratio                  character varying,
    loan_term                         character varying,
    rate_spread_intro                 character varying,
    baloon_payment                    integer,
    insert_only_payment               integer,
    amortization                      integer,
    other_amortization                integer,
    property_value                    character varying,
    home_security_policy              integer,
    lan_property_interest             integer,
    total_uits                        integer,
    mf_affordable                     character varying,
    application_submission            integer,
    payable                           integer,
    nmls                              character varying,
    aus1                              character varying,
    aus2                              character varying,
    aus3                              character varying,
    aus4                              character varying,
    aus5                              character varying,
    other_aus                         character varying,
    aus1_result                       integer,
    aus2_result                       character varying,
    aus3_result                       character varying,
    aus4_result                       character varying,
    aus5_result                       character varying,
    other_aus_result                  character varying,
    reverse_mortgage                  integer,
    line_of_credits                   integer,
    business_or_commercial            integer,
    conforming_loan_limit             character varying,
    ethnicity_categorization          character varying,
    race_categorization               character varying,
    sex_categorization                character varying,
    dwelling_categorization           character varying,
    loan_product_type_categorization  character varying,
    tract_population                  integer,
    tract_minority_population_percent float,
    ffiec_msa_md_median_family_income integer,
    tract_owner_occupied_units        integer,
    tract_one_to_four_family_homes    integer,
    tract_median_age_of_housing_units integer,
    tract_to_msa_income_percentage    float,
    is_quarterly                      boolean                     DEFAULT false NOT NULL, -- New for 2020 and beyond quarterly filing
    created_at                        timestamp without time zone DEFAULT now(),
    msa_md character varying,
    msa_md_name                       character varying
);

create table lar2020_q1 as select * from loanapplicationregister2020 where true = false;
create table lar2020_q2 as select * from loanapplicationregister2020 where true = false;
create table lar2020_q3 as select * from loanapplicationregister2020 where true = false;