<?php

namespace App\Models;

class ChatRoomInfo extends \CodeIgniter\Model
{
    protected $table = 'ChatRoomInfo';
    protected $allowedFields = [
        'chat_room_no',
        'owner_no',
        'chat_room_title',
        'create_date',
        'chat_room_image',
        'chat_room_desc',
        'group_no'
    ];
    protected $primaryKey = 'chat_room_no';
}
