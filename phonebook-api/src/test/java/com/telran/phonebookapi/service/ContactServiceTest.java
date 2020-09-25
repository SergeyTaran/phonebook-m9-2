package com.telran.phonebookapi.service;

import com.telran.phonebookapi.dto.ContactDto;
import com.telran.phonebookapi.model.Contact;
import com.telran.phonebookapi.model.Phone;
import com.telran.phonebookapi.model.User;
import com.telran.phonebookapi.persistance.IContactRepository;
import com.telran.phonebookapi.persistance.IUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.persistence.EntityNotFoundException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class ContactServiceTest {

    @Mock
    IUserRepository userRepository;

    @Mock
    IContactRepository contactRepository;

    @InjectMocks
    ContactService contactService;

    @Test
    public void testAdd_userExists_userWithContact() {

        User user = new User("test@gmail.com", "12345678");
        when(userRepository.findById(user.getEmail())).thenReturn(Optional.of(user));

        contactService.add("test@gmail.com", "firstName", "lastName", "friend");

        verify(contactRepository, times(1)).save(any());
        verify(contactRepository, times(1)).save(argThat(contact ->
                contact.getFirstName().equals("firstName")
                        && contact.getLastName().equals("lastName")
                        && contact.getDescription().equals("friend")
                        && contact.getUser().getEmail().equals("test@gmail.com")
        ));
    }

    @Test
    public void testAdd_userDoesNotExist_EntityNotFoundException() {

        Exception exception = assertThrows(EntityNotFoundException.class, () -> contactService.add("unknown@gmail.com",
                "FirstName",
                "LastName",
                "Description"
        ));

        verify(userRepository, times(1)).findById(anyString());
        assertEquals("Error! This user doesn't exist in our DB", exception.getMessage());
    }

    @Test
    public void testEditContact_contactExist_AllFieldsChanged() {

        User user = new User("test@gmail.com", "12345678");

        Contact oldContact = new Contact("TestName", user);
        ContactDto contactDto = new ContactDto();
        contactDto.firstName = "NewName";
        contactDto.lastName = "NewLastName";
        contactDto.description = "newDescription";

        when(contactRepository.findById(contactDto.id)).thenReturn(Optional.of(oldContact));

        contactService.editContact(contactDto.id, contactDto.firstName, contactDto.lastName, contactDto.description);

        verify(contactRepository, times(1)).save(any());
        verify(contactRepository, times(1)).save(argThat(contact ->
                contact.getFirstName().equals(contactDto.firstName)
                        && contact.getLastName().equals(contactDto.lastName)
                        && contact.getDescription().equals(contactDto.description)
        ));
    }

    @Test
    public void testEditContact_editProfile_AllProfileFieldsChanged() {

        User user = new User("test@gmail.com", "12345678");

        Contact oldProfile = new Contact("TestName", user);
        ContactDto newProfileDto = new ContactDto();
        newProfileDto.firstName = "NewName";
        newProfileDto.lastName = "NewLastName";
        newProfileDto.description = "newDescription";

        when(contactRepository.findById(newProfileDto.id)).thenReturn(Optional.of(oldProfile));

        contactService.editContact(newProfileDto.id, newProfileDto.firstName, newProfileDto.lastName, newProfileDto.description);

        verify(contactRepository, times(1)).save(any());
        verify(contactRepository, times(1)).save(argThat(profile ->
                profile.getFirstName().equals(newProfileDto.firstName)
                        && profile.getLastName().equals(newProfileDto.lastName)
                        && profile.getDescription().equals(newProfileDto.description)
        ));
    }

    @Test
    public void testEditContact_contactDoesNotExist_EntityNotFoundException() {

        ContactDto contactDto = new ContactDto();
        contactDto.firstName = "ContactName";
        contactDto.lastName = "LastName";
        contactDto.description = "Description";

        Exception exception = assertThrows(EntityNotFoundException.class, () -> contactService.editContact(
                contactDto.id,
                contactDto.firstName,
                contactDto.lastName,
                contactDto.description));

        verify(contactRepository, times(1)).findById(any());
        assertEquals("Error! This contact doesn't exist", exception.getMessage());
    }

    @Captor
    ArgumentCaptor<Contact> contactCaptor;

    @Test
    public void testRemoveById_contactExists_ContactDeleted() {

        User user = new User("test@gmail.com", "12345678");
        Contact contact = new Contact("Name", user);
        contact.setLastName("Surname");
        contact.setDescription("person");

        ContactDto contactDto = new ContactDto(1, "Name", "Surname", "person");

        when(contactRepository.findById(contactDto.id)).thenReturn(Optional.of(contact));
        contactService.removeById(contactDto.id);

        List<Contact> capturedContacts = contactCaptor.getAllValues();
        verify(contactRepository, times(1)).deleteById(contactDto.id);
        assertEquals(0, capturedContacts.size());
    }

    @Test
    public void testRemoveById_contactDoesNotExist_EntityNotFoundException() {

        Exception exception = assertThrows(EntityNotFoundException.class, () -> contactService.removeById(-1));

        verify(contactRepository, times(1)).findById(any());
        assertEquals("Error! This contact doesn't exist", exception.getMessage());
    }

    @Test
    public void testGetById_userWithContact_Contact() {
        User user = new User("test@gmail.com", "12345678");
        Contact contact = new Contact("Name", user);
        contact.setLastName("Surname");
        contact.setDescription("person");

        ContactDto contactDto = new ContactDto(1, "Name", "Surname", "person");

        when(contactRepository.findById(contactDto.id)).thenReturn(Optional.of(contact));
        Contact contactFounded = contactService.getById(contactDto.id);

        assertEquals(contactDto.firstName, contactFounded.getFirstName());
        assertEquals(contactDto.lastName, contactFounded.getLastName());
        assertEquals(contactDto.description, contactFounded.getDescription());

        verify(contactRepository, times(1)).findById(argThat(
                id -> id == contactDto.id));
    }

    @Test
    public void testGetById_contactDoesNotExist_EntityNotFoundException() {

        Exception exception = assertThrows(EntityNotFoundException.class, () -> contactService.getById(-1));

        verify(contactRepository, times(1)).findById(any());
        assertEquals("Error! This contact doesn't exist", exception.getMessage());
    }

    @Test
    void testGetAllContactsByUserId_userWithContacts_ListContacts() {
        User user = spy(new User("test@gmail.com", "12345678"));
        Contact contact01 = spy (new Contact("TestName01", user));
        Contact contact02 = spy (new Contact("TestName02", user));
        when(contact01.getId()).thenReturn(1);
        when(contact02.getId()).thenReturn(2);
        when(userRepository.findById("test@gmail.com")).thenReturn(Optional.of(user));
        when(user.getContacts()).thenReturn(Arrays.asList(contact01, contact02));
        when(user.getMyProfile()).thenReturn(new Contact());
        List<Contact> contactsFounded = contactService.getAllContactsByUserId("test@gmail.com");

        assertEquals(2, contactsFounded.size());
        assertEquals(contactsFounded.get(0).getFirstName(), "TestName01");
        assertEquals(contactsFounded.get(1).getFirstName(), "TestName02");
    }

    @Test
    void testGetPhone_contactWithPhones_ListPhones() {
        User user = new User("test@gmail.com", "12345678");
        Contact contact = spy(new Contact("TestName01", user));
        Phone phone = new Phone(49, 12345678, contact);
        Phone phone02 = new Phone(39, 87654321, contact);
        ContactDto contactDto = ContactDto.builder()
                .firstName("TestName01")
                .build();

        when(contactRepository.findById(contactDto.id)).thenReturn(Optional.of(contact));
        when(contact.getPhones()).thenReturn(Arrays.asList(phone, phone02));
        List<Phone> phonesFounded = contactService.getPhones(contactDto.id);

        assertEquals(phonesFounded.size(), 2);
        assertEquals(phonesFounded.get(0).getCountryCode(), 49);
        assertEquals(phonesFounded.get(1).getPhoneNumber(), 87654321);

        verify(contactRepository, times(1)).findById(any());
    }

}
