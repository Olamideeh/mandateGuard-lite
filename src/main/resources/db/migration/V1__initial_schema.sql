--
-- PostgreSQL database dump
--


-- Dumped from database version 16.14
-- Dumped by pg_dump version 16.14

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: agent_payment_requests; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.agent_payment_requests (
    id uuid NOT NULL,
    agent_request_timestamp timestamp(6) with time zone NOT NULL,
    amount numeric(19,2) NOT NULL,
    approved_at timestamp(6) with time zone,
    created_at timestamp(6) with time zone NOT NULL,
    currency_code character varying(3) NOT NULL,
    decided_at timestamp(6) with time zone,
    decision character varying(30),
    decision_explanation character varying(1000),
    description character varying(500),
    executed_at timestamp(6) with time zone,
    idempotency_key character varying(100) NOT NULL,
    merchant_category_code character varying(4) NOT NULL,
    merchant_country_code character varying(2) NOT NULL,
    merchant_identifier character varying(150) NOT NULL,
    merchant_name character varying(150) NOT NULL,
    merchant_verified boolean NOT NULL,
    provider_reference character varying(150),
    reason_code character varying(50),
    reference character varying(80) NOT NULL,
    request_nonce character varying(100) NOT NULL,
    request_payload_hash character varying(64) NOT NULL,
    request_signature text NOT NULL,
    status character varying(30) NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    version bigint NOT NULL,
    agent_id uuid NOT NULL,
    mandate_id uuid NOT NULL,
    CONSTRAINT agent_payment_requests_decision_check CHECK (((decision)::text = ANY ((ARRAY['ALLOWED'::character varying, 'DENIED'::character varying, 'REQUIRES_APPROVAL'::character varying])::text[]))),
    CONSTRAINT agent_payment_requests_reason_code_check CHECK (((reason_code)::text = ANY ((ARRAY['ALL_RULES_PASSED'::character varying, 'MANDATE_NOT_ACTIVE'::character varying, 'MANDATE_EXPIRED'::character varying, 'MANDATE_REVOKED'::character varying, 'MANDATE_EXHAUSTED'::character varying, 'AGENT_NOT_AUTHORIZED'::character varying, 'CURRENCY_NOT_ALLOWED'::character varying, 'COUNTRY_NOT_ALLOWED'::character varying, 'MERCHANT_NOT_ALLOWED'::character varying, 'MERCHANT_CATEGORY_NOT_ALLOWED'::character varying, 'SINGLE_AMOUNT_LIMIT_EXCEEDED'::character varying, 'TOTAL_BUDGET_EXCEEDED'::character varying, 'TRANSACTION_LIMIT_EXCEEDED'::character varying, 'HUMAN_APPROVAL_REQUIRED'::character varying, 'PRINCIPAL_REJECTED'::character varying, 'RISK_HOLD'::character varying, 'IDEMPOTENCY_CONFLICT'::character varying])::text[]))),
    CONSTRAINT agent_payment_requests_status_check CHECK (((status)::text = ANY ((ARRAY['RECEIVED'::character varying, 'EVALUATING'::character varying, 'AWAITING_APPROVAL'::character varying, 'APPROVED'::character varying, 'REJECTED'::character varying, 'AUTHORIZED'::character varying, 'EXECUTED'::character varying, 'FAILED'::character varying])::text[])))
);


--
-- Name: ai_agents; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.ai_agents (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    external_agent_id character varying(100) NOT NULL,
    last_authenticated_at timestamp(6) with time zone,
    name character varying(150) NOT NULL,
    provider character varying(100) NOT NULL,
    public_key_fingerprint character varying(64) NOT NULL,
    public_key_pem text NOT NULL,
    status character varying(20) NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    principal_id uuid NOT NULL,
    CONSTRAINT ai_agents_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'ACTIVE'::character varying, 'SUSPENDED'::character varying, 'REVOKED'::character varying])::text[])))
);


--
-- Name: audit_events; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.audit_events (
    id uuid NOT NULL,
    actor_id character varying(100),
    actor_type character varying(30) NOT NULL,
    correlation_id character varying(100) NOT NULL,
    details text NOT NULL,
    entity_id character varying(100) NOT NULL,
    entity_type character varying(50) NOT NULL,
    event_type character varying(50) NOT NULL,
    new_state character varying(50),
    occurred_at timestamp(6) with time zone NOT NULL,
    previous_state character varying(50),
    principal_id uuid NOT NULL,
    CONSTRAINT audit_events_actor_type_check CHECK (((actor_type)::text = ANY ((ARRAY['PRINCIPAL'::character varying, 'AI_AGENT'::character varying, 'RISK_OFFICER'::character varying, 'ADMIN'::character varying, 'SYSTEM'::character varying])::text[]))),
    CONSTRAINT audit_events_event_type_check CHECK (((event_type)::text = ANY ((ARRAY['PRINCIPAL_REGISTERED'::character varying, 'AGENT_REGISTERED'::character varying, 'AGENT_ACTIVATED'::character varying, 'AGENT_SUSPENDED'::character varying, 'MANDATE_CREATED'::character varying, 'MANDATE_ACTIVATED'::character varying, 'MANDATE_SUSPENDED'::character varying, 'MANDATE_REVOKED'::character varying, 'PAYMENT_REQUEST_RECEIVED'::character varying, 'SIGNATURE_VERIFIED'::character varying, 'SIGNATURE_REJECTED'::character varying, 'PAYMENT_ALLOWED'::character varying, 'PAYMENT_DENIED'::character varying, 'APPROVAL_REQUESTED'::character varying, 'PAYMENT_APPROVED'::character varying, 'PAYMENT_REJECTED'::character varying, 'PAYMENT_EXECUTED'::character varying, 'PAYMENT_FAILED'::character varying, 'RISK_HOLD_CREATED'::character varying])::text[])))
);


--
-- Name: mandate_allowed_categories; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.mandate_allowed_categories (
    mandate_id uuid NOT NULL,
    merchant_category_code character varying(4) NOT NULL
);


--
-- Name: mandate_allowed_countries; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.mandate_allowed_countries (
    mandate_id uuid NOT NULL,
    country_code character varying(2) NOT NULL
);


--
-- Name: mandate_allowed_merchants; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.mandate_allowed_merchants (
    mandate_id uuid NOT NULL,
    merchant_identifier character varying(150) NOT NULL
);


--
-- Name: payment_approvals; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.payment_approvals (
    id uuid NOT NULL,
    decided_at timestamp(6) with time zone,
    decision_note character varying(1000),
    expires_at timestamp(6) with time zone NOT NULL,
    requested_at timestamp(6) with time zone NOT NULL,
    status character varying(20) NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    version bigint NOT NULL,
    payment_request_id uuid NOT NULL,
    principal_id uuid NOT NULL,
    CONSTRAINT payment_approvals_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'APPROVED'::character varying, 'REJECTED'::character varying, 'EXPIRED'::character varying])::text[])))
);


--
-- Name: payment_mandates; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.payment_mandates (
    id uuid NOT NULL,
    allow_any_verified_merchant boolean NOT NULL,
    approval_threshold numeric(19,2),
    consumed_amount numeric(19,2) NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    currency_code character varying(3) NOT NULL,
    expires_at timestamp(6) with time zone NOT NULL,
    maximum_single_amount numeric(19,2) NOT NULL,
    maximum_transactions integer NOT NULL,
    name character varying(150) NOT NULL,
    reference character varying(80) NOT NULL,
    status character varying(20) NOT NULL,
    total_budget numeric(19,2) NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    used_transaction_count integer NOT NULL,
    valid_from timestamp(6) with time zone NOT NULL,
    version bigint NOT NULL,
    agent_id uuid NOT NULL,
    principal_id uuid NOT NULL,
    CONSTRAINT payment_mandates_status_check CHECK (((status)::text = ANY ((ARRAY['DRAFT'::character varying, 'ACTIVE'::character varying, 'SUSPENDED'::character varying, 'REVOKED'::character varying, 'EXPIRED'::character varying, 'EXHAUSTED'::character varying])::text[])))
);


--
-- Name: principals; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.principals (
    id uuid NOT NULL,
    active boolean NOT NULL,
    country_code character varying(2) NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    email character varying(254) NOT NULL,
    name character varying(150) NOT NULL,
    password_hash character varying(100) NOT NULL,
    type character varying(20) NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    CONSTRAINT principals_type_check CHECK (((type)::text = ANY ((ARRAY['INDIVIDUAL'::character varying, 'BUSINESS'::character varying])::text[])))
);


--
-- Name: agent_payment_requests agent_payment_requests_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.agent_payment_requests
    ADD CONSTRAINT agent_payment_requests_pkey PRIMARY KEY (id);


--
-- Name: ai_agents ai_agents_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ai_agents
    ADD CONSTRAINT ai_agents_pkey PRIMARY KEY (id);


--
-- Name: audit_events audit_events_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.audit_events
    ADD CONSTRAINT audit_events_pkey PRIMARY KEY (id);


--
-- Name: mandate_allowed_categories mandate_allowed_categories_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mandate_allowed_categories
    ADD CONSTRAINT mandate_allowed_categories_pkey PRIMARY KEY (mandate_id, merchant_category_code);


--
-- Name: mandate_allowed_countries mandate_allowed_countries_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mandate_allowed_countries
    ADD CONSTRAINT mandate_allowed_countries_pkey PRIMARY KEY (mandate_id, country_code);


--
-- Name: mandate_allowed_merchants mandate_allowed_merchants_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mandate_allowed_merchants
    ADD CONSTRAINT mandate_allowed_merchants_pkey PRIMARY KEY (mandate_id, merchant_identifier);


--
-- Name: payment_approvals payment_approvals_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payment_approvals
    ADD CONSTRAINT payment_approvals_pkey PRIMARY KEY (id);


--
-- Name: payment_mandates payment_mandates_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payment_mandates
    ADD CONSTRAINT payment_mandates_pkey PRIMARY KEY (id);


--
-- Name: principals principals_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.principals
    ADD CONSTRAINT principals_pkey PRIMARY KEY (id);


--
-- Name: agent_payment_requests uk_agent_idempotency_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.agent_payment_requests
    ADD CONSTRAINT uk_agent_idempotency_key UNIQUE (agent_id, idempotency_key);


--
-- Name: ai_agents uk_agent_key_fingerprint; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ai_agents
    ADD CONSTRAINT uk_agent_key_fingerprint UNIQUE (public_key_fingerprint);


--
-- Name: agent_payment_requests uk_agent_request_nonce; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.agent_payment_requests
    ADD CONSTRAINT uk_agent_request_nonce UNIQUE (agent_id, request_nonce);


--
-- Name: payment_approvals uk_approval_payment_request; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payment_approvals
    ADD CONSTRAINT uk_approval_payment_request UNIQUE (payment_request_id);


--
-- Name: payment_mandates uk_mandate_reference; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payment_mandates
    ADD CONSTRAINT uk_mandate_reference UNIQUE (reference);


--
-- Name: agent_payment_requests uk_payment_request_reference; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.agent_payment_requests
    ADD CONSTRAINT uk_payment_request_reference UNIQUE (reference);


--
-- Name: principals uk_principal_email; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.principals
    ADD CONSTRAINT uk_principal_email UNIQUE (email);


--
-- Name: ai_agents uk_principal_external_agent; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ai_agents
    ADD CONSTRAINT uk_principal_external_agent UNIQUE (principal_id, external_agent_id);


--
-- Name: idx_audit_correlation_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audit_correlation_id ON public.audit_events USING btree (correlation_id);


--
-- Name: idx_audit_entity; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audit_entity ON public.audit_events USING btree (entity_type, entity_id);


--
-- Name: idx_audit_principal; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audit_principal ON public.audit_events USING btree (principal_id);


--
-- Name: ai_agents fk_agent_principal; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ai_agents
    ADD CONSTRAINT fk_agent_principal FOREIGN KEY (principal_id) REFERENCES public.principals(id);


--
-- Name: payment_approvals fk_approval_payment_request; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payment_approvals
    ADD CONSTRAINT fk_approval_payment_request FOREIGN KEY (payment_request_id) REFERENCES public.agent_payment_requests(id);


--
-- Name: payment_approvals fk_approval_principal; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payment_approvals
    ADD CONSTRAINT fk_approval_principal FOREIGN KEY (principal_id) REFERENCES public.principals(id);


--
-- Name: payment_mandates fk_mandate_agent; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payment_mandates
    ADD CONSTRAINT fk_mandate_agent FOREIGN KEY (agent_id) REFERENCES public.ai_agents(id);


--
-- Name: payment_mandates fk_mandate_principal; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payment_mandates
    ADD CONSTRAINT fk_mandate_principal FOREIGN KEY (principal_id) REFERENCES public.principals(id);


--
-- Name: agent_payment_requests fk_request_agent; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.agent_payment_requests
    ADD CONSTRAINT fk_request_agent FOREIGN KEY (agent_id) REFERENCES public.ai_agents(id);


--
-- Name: agent_payment_requests fk_request_mandate; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.agent_payment_requests
    ADD CONSTRAINT fk_request_mandate FOREIGN KEY (mandate_id) REFERENCES public.payment_mandates(id);


--
-- Name: mandate_allowed_merchants fkdqm2si0470a8r8vv3mv6ie9b8; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mandate_allowed_merchants
    ADD CONSTRAINT fkdqm2si0470a8r8vv3mv6ie9b8 FOREIGN KEY (mandate_id) REFERENCES public.payment_mandates(id);


--
-- Name: mandate_allowed_categories fkmyrc8mrvefkc78k2g57h2oade; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mandate_allowed_categories
    ADD CONSTRAINT fkmyrc8mrvefkc78k2g57h2oade FOREIGN KEY (mandate_id) REFERENCES public.payment_mandates(id);


--
-- Name: mandate_allowed_countries fktkfj7twdc740uxldkpe38yy2u; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mandate_allowed_countries
    ADD CONSTRAINT fktkfj7twdc740uxldkpe38yy2u FOREIGN KEY (mandate_id) REFERENCES public.payment_mandates(id);


--
-- PostgreSQL database dump complete
--


