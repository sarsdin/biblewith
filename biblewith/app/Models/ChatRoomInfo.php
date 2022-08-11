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
        'chat_room_image'
    ];
    protected $primaryKey = 'chat_room_no';
}
